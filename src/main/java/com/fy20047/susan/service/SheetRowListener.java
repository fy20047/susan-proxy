package com.fy20047.susan.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheetRowListener extends AnalysisEventListener<SheetRowDto> {

    private static final Logger log = LoggerFactory.getLogger(SheetRowListener.class);
    private static final Set<String> SETTINGS_SHEET_NAMES = Set.of("設定", "分頁");
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "對帳", "定金80%", "尾款20%", "購買總額", "團友", "品項", "日幣原價", "已採購", "出貨狀態"
    );
    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");

    private final SheetSyncWriter sheetSyncWriter;
    private final boolean streamByBuyer;
    private final int maxRowsWarn;
    private final int itemBatchSize;
    private final Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();
    private final Map<String, Long> groupIdByBuyer = new LinkedHashMap<>();
    private final Map<Long, Integer> bonusByGroupId = new LinkedHashMap<>();
    private final List<OrderItem> itemBuffer = new ArrayList<>();
    private final Set<String> processedSheets = new HashSet<>();
    private int totalGroupsSaved = 0;
    private final Set<String> visibleSheets;
    private String currentSheetName = "";
    private boolean validSheet = false;
    private boolean skipCurrentSheet = false;
    private int rowCount = 0;
    private boolean warnedMaxRows = false;
    private boolean preparedReplace = false;
    private boolean hasData = false;
    private String lastBuyerNickname = "";

    public SheetRowListener(SheetSyncWriter sheetSyncWriter) {
        this(sheetSyncWriter, null, false, 0, 200);
    }

    public SheetRowListener(SheetSyncWriter sheetSyncWriter, Set<String> visibleSheets) {
        this(sheetSyncWriter, visibleSheets, false, 0, 200);
    }

    public SheetRowListener(
            SheetSyncWriter sheetSyncWriter,
            Set<String> visibleSheets,
            boolean streamByBuyer,
            int maxRowsWarn,
            int itemBatchSize) {
        this.sheetSyncWriter = sheetSyncWriter;
        this.visibleSheets = visibleSheets;
        this.streamByBuyer = streamByBuyer;
        this.maxRowsWarn = maxRowsWarn;
        this.itemBatchSize = Math.max(1, itemBatchSize);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        currentSheetName = context.readSheetHolder().getSheetName();
        groupByBuyer.clear();
        groupIdByBuyer.clear();
        bonusByGroupId.clear();
        itemBuffer.clear();
        rowCount = 0;
        warnedMaxRows = false;
        preparedReplace = false;
        hasData = false;
        lastBuyerNickname = "";
        validSheet = false;
        skipCurrentSheet = shouldSkipSheet(currentSheetName);

        if (skipCurrentSheet) {
            log.info("略過分頁 {}", currentSheetName);
            return;
        }

        if (headMap == null || headMap.isEmpty()) {
            log.warn("分頁 {} 找不到欄位表頭", currentSheetName);
            return;
        }

        Set<String> headers = new HashSet<>();
        for (String header : headMap.values()) {
            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
            }
        }

        if (!headers.containsAll(REQUIRED_HEADERS)) {
            log.warn("分頁 {} 欄位不足，略過同步，當前欄位: {}", currentSheetName, headers);
            return;
        }

        validSheet = true;
    }

    @Override
    public void invoke(SheetRowDto row, AnalysisContext context) {
        if (!validSheet) {
            return;
        }
        try {

        rowCount += 1;
        if (!warnedMaxRows && maxRowsWarn > 0 && rowCount > maxRowsWarn) {
            warnedMaxRows = true;
            log.warn("分頁 {} 資料列數已超過 {} 筆，建議分批寫入或提高記憶體限制", currentSheetName, maxRowsWarn);
        }

        String itemName = safeString(row.getItemName());
        if (itemName.isEmpty()) {
            return;
        }

        String buyerNickname = safeString(row.getBuyerNickname());
        if (buyerNickname.isEmpty()) {
            buyerNickname = lastBuyerNickname;
        } else {
            lastBuyerNickname = buyerNickname;
        }
        if (buyerNickname.isEmpty()) {
            return;
        }

        if (streamByBuyer && !preparedReplace) {
            sheetSyncWriter.prepareReplace(currentSheetName);
            preparedReplace = true;
        }

        OrderGroup group;
        Long groupId = null;
        if (streamByBuyer) {
            groupId = groupIdByBuyer.get(buyerNickname);
            if (groupId == null) {
                OrderGroup newGroup = new OrderGroup();
                newGroup.setBuyerNickname(buyerNickname);
                newGroup.setGroupName(currentSheetName);
                newGroup.setLastUpdated(LocalDateTime.now());
                groupId = sheetSyncWriter.createGroup(newGroup);
                groupIdByBuyer.put(buyerNickname, groupId);
                totalGroupsSaved += 1;
                hasData = true;
            }
            group = null;
        } else {
            group = groupByBuyer.computeIfAbsent(buyerNickname, key -> {
                OrderGroup newGroup = new OrderGroup();
                newGroup.setBuyerNickname(key);
                newGroup.setGroupName(currentSheetName);
                newGroup.setLastUpdated(LocalDateTime.now());
                return newGroup;
            });
        }

        Integer bonus = parseInteger(row.getBonus());
        if (bonus != null) {
            if (streamByBuyer) {
                int current = bonusByGroupId.getOrDefault(groupId, 0);
                if (bonus > current) {
                    bonusByGroupId.put(groupId, bonus);
                }
            } else {
                int current = group.getBonusCount() == null ? 0 : group.getBonusCount();
                if (bonus > current) {
                    group.setBonusCount(bonus);
                }
            }
        }

        OrderItem item = new OrderItem();
        String orderSn = safeString(row.getOrderRank());
        if (orderSn.isEmpty()) {
            orderSn = safeString(row.getOrderSn());
        }
        item.setOrderSn(orderSn);
        item.setQueued(parseBoolean(row.getQueued()));
        item.setCheckedIn(parseBoolean(row.getCheckedIn()));
        item.setBalanceDueDate(safeString(row.getBalanceDueDate()));
        String depositPaidDate = safeString(row.getDepositPaidDate());
        item.setDepositPaidDate(depositPaidDate);
        String checkMark = safeString(row.getCheckMark());
        if (checkMark.isEmpty() && !depositPaidDate.isEmpty()) {
            checkMark = depositPaidDate;
        }
        item.setCheckMark(checkMark);
        item.setDepositAmount(defaultInt(row.getDepositAmount()));
        item.setBalanceAmount(defaultInt(row.getBalanceAmount()));
        item.setTotalAmount(defaultInt(row.getTotalAmount()));
        item.setItemName(itemName);
        item.setQuantity(defaultInt(row.getQuantity(), 1));
        item.setJpyPrice(row.getJpyPrice());

        boolean isReconciled = parseBoolean(row.getReconciled());
        boolean isPurchased = parseBoolean(row.getPurchased());
        boolean isArrived = parseBoolean(row.getArrived());
        boolean isShipped = parseBoolean(row.getShipped());
        item.setItemStatus(StatusResolver.determine(isReconciled, isPurchased, isArrived, isShipped));

        if (streamByBuyer) {
            OrderGroup groupRef = new OrderGroup();
            groupRef.setId(groupId);
            item.setOrderGroup(groupRef);
            itemBuffer.add(item);
            if (itemBuffer.size() >= itemBatchSize) {
                flushItems("batch");
                itemBuffer.clear();
            }
        } else {
            group.addItem(item);
        }
        } catch (Exception e) {
            int rowIndex = context.readRowHolder() == null ? -1 : context.readRowHolder().getRowIndex();
            int displayRow = rowIndex >= 0 ? rowIndex + 1 : -1;
            log.warn("Sheet {} row parse/save failed (rowIndex={}, rowCount={})", currentSheetName, displayRow, rowCount, e);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!validSheet) {
            return;
        }

        if (streamByBuyer) {
            if (!itemBuffer.isEmpty()) {
                flushItems("final");
                itemBuffer.clear();
            }
            if (!bonusByGroupId.isEmpty()) {
                sheetSyncWriter.updateGroupBonuses(bonusByGroupId);
            }
            if (!hasData) {
                return;
            }
        } else {
            if (groupByBuyer.isEmpty()) {
                return;
            }
            sheetSyncWriter.replaceGroups(currentSheetName, groupByBuyer.values());
            totalGroupsSaved += groupByBuyer.size();
        }

        log.info("Sheet {} processed rows={} groupsSaved={}", currentSheetName, rowCount, totalGroupsSaved);
        processedSheets.add(SheetNameNormalizer.normalize(currentSheetName));
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        int rowIndex = context.readRowHolder() == null ? -1 : context.readRowHolder().getRowIndex();
        int displayRow = rowIndex >= 0 ? rowIndex + 1 : -1;
        log.warn("Sheet {} row parse exception (rowIndex={}, rowCount={})", currentSheetName, displayRow, rowCount, exception);
    }

    public Set<String> getProcessedSheets() {
        return processedSheets;
    }

    public int getTotalGroupsSaved() {
        return totalGroupsSaved;
    }

    private boolean shouldSkipSheet(String sheetName) {
        if (sheetName != null && SETTINGS_SHEET_NAMES.contains(sheetName)) {
            return true;
        }
        if (visibleSheets == null) {
            return false;
        }
        String normalized = SheetNameNormalizer.normalize(sheetName);
        if (normalized.isEmpty()) {
            return true;
        }
        return !visibleSheets.contains(normalized);
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean parseBoolean(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        return TRUE_VALUES.contains(normalized);
    }

    private int defaultInt(Integer value) {
        return defaultInt(value, 0);
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer parseInteger(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().replace(",", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void flushItems(String phase) {
        try {
            sheetSyncWriter.saveItems(itemBuffer);
        } catch (Exception e) {
            log.warn("分頁 {} 寫入 {} 批次失敗，將改為逐筆寫入 (rowCount={})",
                    currentSheetName, phase, rowCount, e);
            for (OrderItem item : itemBuffer) {
                try {
                    sheetSyncWriter.saveItem(item);
                } catch (Exception ex) {
                    log.warn("分頁 {} 單筆寫入失敗 (rowCount={}, buyer={}, item={})",
                            currentSheetName, rowCount,
                            safeString(item.getOrderGroup() == null ? "" : item.getOrderGroup().getBuyerNickname()),
                            safeString(item.getItemName()),
                            ex);
                }
            }
        }
    }

}

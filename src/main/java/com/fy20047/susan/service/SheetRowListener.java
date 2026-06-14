package com.fy20047.susan.service;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fy20047.susan.domain.GroupSourceType;
import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.domain.ShippingStatus;
import com.fy20047.susan.service.SheetSyncService.SyncWarning;
import java.lang.reflect.Field;
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
    private static final Set<String> SETTINGS_SHEET_NAMES = Set.of("設定");
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "對帳", "定金80%", "尾款20%", "購買總額", "團友", "品項");
    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");
    private static final String QUEUED_HEADER = resolveExcelHeader("queued");
    private static final String LEGACY_QUEUED_HEADER = resolveExcelHeader("legacyQueued");
    private static final String PREORDER_STATUS_HEADER = resolveExcelHeader("preorderStatus");
    private static final String NOT_CHECKED_IN_HEADER = resolveExcelHeader("notCheckedIn");

    private final SheetSyncWriter sheetSyncWriter;
    private final String sourceKey;
    private final GroupSourceType defaultSourceType;
    private final Map<String, SheetSyncSheetConfig> sheetConfigs;
    private final boolean streamByBuyer;
    private final int maxRowsWarn;
    private final int itemBatchSize;
    private final int sheetNameMatchMaxCompareLength;
    private final int sheetNameMatchMinCompareLength;
    private final LocalDateTime syncTimestamp;
    private final Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();
    private final Map<String, Long> groupIdByBuyer = new LinkedHashMap<>();
    private final Map<Long, Integer> bonusByGroupId = new LinkedHashMap<>();
    private final List<OrderItem> itemBuffer = new ArrayList<>();
    private final List<SyncWarning> warnings = new ArrayList<>();
    private final Set<String> processedSheets = new HashSet<>();

    private int totalGroupsSaved = 0;
    private String currentSheetName = "";
    private GroupSourceType currentSourceType = GroupSourceType.STANDARD;
    private boolean validSheet = false;
    private boolean skipCurrentSheet = false;
    private int rowCount = 0;
    private boolean warnedMaxRows = false;
    private boolean preparedReplace = false;
    private boolean hasData = false;
    private String lastBuyerNickname = "";
    private boolean hasQueuedColumn = false;
    private boolean hasLegacyQueuedColumn = false;
    private boolean hasPreorderStatusColumn = false;
    private boolean hasNotCheckedInColumn = false;

    public SheetRowListener(SheetSyncWriter sheetSyncWriter) {
        this(sheetSyncWriter, null, GroupSourceType.STANDARD, null, false, 0, 200, 64, 16);
    }

    public SheetRowListener(SheetSyncWriter sheetSyncWriter, Set<String> visibleSheets) {
        this(sheetSyncWriter, null, GroupSourceType.STANDARD, toSheetConfigs(visibleSheets), false, 0, 200, 64, 16);
    }

    public SheetRowListener(
            SheetSyncWriter sheetSyncWriter,
            Set<String> visibleSheets,
            boolean streamByBuyer,
            int maxRowsWarn,
            int itemBatchSize,
            int sheetNameMatchMaxCompareLength,
            int sheetNameMatchMinCompareLength) {
        this(
                sheetSyncWriter,
                null,
                GroupSourceType.STANDARD,
                toSheetConfigs(visibleSheets),
                streamByBuyer,
                maxRowsWarn,
                itemBatchSize,
                sheetNameMatchMaxCompareLength,
                sheetNameMatchMinCompareLength);
    }

    public SheetRowListener(
            SheetSyncWriter sheetSyncWriter,
            String sourceKey,
            GroupSourceType defaultSourceType,
            Map<String, SheetSyncSheetConfig> sheetConfigs,
            boolean streamByBuyer,
            int maxRowsWarn,
            int itemBatchSize,
            int sheetNameMatchMaxCompareLength,
            int sheetNameMatchMinCompareLength) {
        this(
                sheetSyncWriter,
                sourceKey,
                defaultSourceType,
                sheetConfigs,
                streamByBuyer,
                maxRowsWarn,
                itemBatchSize,
                sheetNameMatchMaxCompareLength,
                sheetNameMatchMinCompareLength,
                LocalDateTime.now());
    }

    public SheetRowListener(
            SheetSyncWriter sheetSyncWriter,
            String sourceKey,
            GroupSourceType defaultSourceType,
            Map<String, SheetSyncSheetConfig> sheetConfigs,
            boolean streamByBuyer,
            int maxRowsWarn,
            int itemBatchSize,
            int sheetNameMatchMaxCompareLength,
            int sheetNameMatchMinCompareLength,
            LocalDateTime syncTimestamp) {
        this.sheetSyncWriter = sheetSyncWriter;
        this.sourceKey = sourceKey;
        this.defaultSourceType = defaultSourceType == null ? GroupSourceType.STANDARD : defaultSourceType;
        this.sheetConfigs = sheetConfigs;
        this.streamByBuyer = streamByBuyer;
        this.maxRowsWarn = maxRowsWarn;
        this.itemBatchSize = Math.max(1, itemBatchSize);
        this.sheetNameMatchMaxCompareLength = Math.max(1, sheetNameMatchMaxCompareLength);
        this.sheetNameMatchMinCompareLength = Math.max(1, sheetNameMatchMinCompareLength);
        this.syncTimestamp = syncTimestamp == null ? LocalDateTime.now() : syncTimestamp;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        currentSheetName = context.readSheetHolder().getSheetName();
        currentSourceType = defaultSourceType;
        groupByBuyer.clear();
        groupIdByBuyer.clear();
        bonusByGroupId.clear();
        itemBuffer.clear();
        rowCount = 0;
        warnedMaxRows = false;
        preparedReplace = false;
        hasData = false;
        lastBuyerNickname = "";
        hasQueuedColumn = false;
        hasLegacyQueuedColumn = false;
        hasPreorderStatusColumn = false;
        hasNotCheckedInColumn = false;
        validSheet = false;
        skipCurrentSheet = shouldSkipSheet(currentSheetName);

        if (skipCurrentSheet) {
            log.info("Skip sheet {}", currentSheetName);
            return;
        }

        if (headMap == null || headMap.isEmpty()) {
            log.warn("Sheet {} has no header row.", currentSheetName);
            return;
        }

        Set<String> headers = new HashSet<>();
        for (String header : headMap.values()) {
            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
            }
        }

        if (!headers.containsAll(REQUIRED_HEADERS)) {
            log.warn("Sheet {} is missing required headers: {}", currentSheetName, headers);
            return;
        }

        hasQueuedColumn = containsHeader(headers, QUEUED_HEADER);
        hasLegacyQueuedColumn = containsHeader(headers, LEGACY_QUEUED_HEADER);
        hasPreorderStatusColumn = containsHeader(headers, PREORDER_STATUS_HEADER);
        hasNotCheckedInColumn = containsHeader(headers, NOT_CHECKED_IN_HEADER);
        currentSourceType = resolveCurrentSourceType(resolveSheetConfig(currentSheetName), hasPreorderStatusColumn);
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
                log.warn("Sheet {} exceeded maxRowsWarn={}", currentSheetName, maxRowsWarn);
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
                sheetSyncWriter.prepareReplace(currentSheetName, sourceKey);
                preparedReplace = true;
            }

            OrderGroup group;
            Long groupId = null;
            if (streamByBuyer) {
                groupId = groupIdByBuyer.get(buyerNickname);
                if (groupId == null) {
                    OrderGroup newGroup = buildGroupSkeleton(buyerNickname);
                    groupId = sheetSyncWriter.createGroup(newGroup);
                    groupIdByBuyer.put(buyerNickname, groupId);
                    totalGroupsSaved += 1;
                    hasData = true;
                }
                group = null;
            } else {
                group = groupByBuyer.computeIfAbsent(buyerNickname, key -> buildGroupSkeleton(key));
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
            item.setOrderRank(safeString(row.getOrderRank()));
            item.setOrderSn(safeString(row.getOrderSn()));
            item.setQueued(resolveQueued(row));
            item.setCheckedIn(hasNotCheckedInColumn && parseBoolean(row.getNotCheckedIn()));
            item.setBalanceDueDate(safeString(row.getBalanceDueDate()));
            String depositPaidDate = safeString(row.getDepositPaidDate());
            String depositConfirmation = safeString(row.getCheckedIn());
            boolean isDepositPaid = !depositPaidDate.isEmpty() || !depositConfirmation.isEmpty();
            if (depositPaidDate.isEmpty() != depositConfirmation.isEmpty()) {
                addWarning(context, buyerNickname, itemName, "付定日與對只有其中一欄有內容，已視為已付訂金。");
            }
            item.setDepositPaidDate(depositPaidDate);
            item.setDepositPaid(isDepositPaid);
            item.setDepositReconciled(parseBoolean(row.getReconciled()));
            boolean isPurchased = parseBoolean(row.getPurchased());
            item.setPurchased(isPurchased);
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
            ShippingStatus shippingStatus = resolveShippingStatus(row);
            item.setItemStatus(resolveItemStatus(row, itemName, isPurchased, isDepositPaid, shippingStatus));
            item.setShippingStatus(shippingStatus);
            validateShippingProgress(row, context, buyerNickname, itemName, isPurchased, isDepositPaid, shippingStatus);

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
            sheetSyncWriter.replaceGroups(currentSheetName, sourceKey, groupByBuyer.values());
            totalGroupsSaved += groupByBuyer.size();
        }

        log.info("Sheet {} processed rows={} groupsSaved={} sourceType={}",
                currentSheetName, rowCount, totalGroupsSaved, currentSourceType);
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

    public List<SyncWarning> getWarnings() {
        return List.copyOf(warnings);
    }

    private void validateShippingProgress(
            SheetRowDto row,
            AnalysisContext context,
            String buyerNickname,
            String itemName,
            boolean isPurchased,
            boolean isDepositPaid,
            ShippingStatus shippingStatus) {
        String shippingProgress = safeString(row.getShippingProgress());
        if (!shippingProgress.isEmpty()) {
            if ("已抵台可出貨".equals(shippingProgress.trim())) {
                addWarning(context, buyerNickname, itemName, "出貨進度「已抵台可出貨」應改為「已抵台待出貨」，已暫時視為可出貨。");
            } else if (!isKnownShippingProgress(shippingProgress, currentSourceType)) {
                addWarning(context, buyerNickname, itemName, "出貨進度值無法辨識：「" + shippingProgress + "」，已暫時視為尚未抵台。");
            }

            if (currentSourceType == GroupSourceType.STANDARD) {
                ShippingStatus legacyStatus = StatusResolver.determineShipping(
                        parseBoolean(row.getArrived()),
                        parseBoolean(row.getShipped()));
                if ((parseBoolean(row.getArrived()) || parseBoolean(row.getShipped())) && legacyStatus != shippingStatus) {
                    addWarning(context, buyerNickname, itemName, "出貨進度與舊欄位「抵台 / 出貨狀態」不一致，已優先採用出貨進度。");
                }
            } else {
                String preorderStatus = safeString(row.getPreorderStatus());
                if (!preorderStatus.isEmpty()) {
                    ShippingStatus legacyStatus = StatusResolver.determinePreorderShipping(preorderStatus, parseBoolean(row.getShipped()));
                    if (legacyStatus != shippingStatus) {
                        addWarning(context, buyerNickname, itemName, "出貨進度與舊欄位「貨況」不一致，已優先採用出貨進度。");
                    }
                }
            }
        }

        boolean advancedShippingProgress =
                shippingStatus == ShippingStatus.READY_TO_SHIP
                        || shippingStatus == ShippingStatus.SHIPPED
                        || "官方已發貨".equals(shippingProgress.trim());
        if (advancedShippingProgress && (!isPurchased || !isDepositPaid)) {
            addWarning(context, buyerNickname, itemName, "出貨進度已進入出貨階段，但已採購或訂金資訊不完整。");
        }
    }

    private boolean isKnownShippingProgress(String rawValue, GroupSourceType sourceType) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (sourceType == GroupSourceType.PREORDER) {
            return Set.of("已下單待發貨", "官方已發貨", "已抵台待出貨", "已抵台可出貨", "已出貨")
                    .contains(value);
        }
        return Set.of("尚未抵台", "未抵台", "已抵台待出貨", "已抵台可出貨", "已出貨")
                .contains(value);
    }

    private void addWarning(
            AnalysisContext context,
            String buyerNickname,
            String itemName,
            String message) {
        warnings.add(new SyncWarning(
                sourceKey,
                currentSheetName,
                resolveDisplayRowNumber(context),
                buyerNickname,
                itemName,
                message));
    }

    private int resolveDisplayRowNumber(AnalysisContext context) {
        if (context == null || context.readRowHolder() == null) {
            return rowCount + 1;
        }
        int rowIndex = context.readRowHolder().getRowIndex();
        return rowIndex >= 0 ? rowIndex + 1 : rowCount + 1;
    }

    private OrderGroup buildGroupSkeleton(String buyerNickname) {
        OrderGroup newGroup = new OrderGroup();
        newGroup.setBuyerNickname(buyerNickname);
        newGroup.setGroupName(currentSheetName);
        newGroup.setSourceKey(sourceKey);
        newGroup.setSourceType(currentSourceType);
        newGroup.setLastUpdated(syncTimestamp);
        return newGroup;
    }

    private ItemStatus resolveItemStatus(
            SheetRowDto row,
            String itemName,
            boolean isPurchased,
            boolean isDepositPaid,
            ShippingStatus shippingStatus) {
        if (currentSourceType == GroupSourceType.PREORDER) {
            return StatusResolver.determinePreorder(
                    itemName,
                    isPurchased,
                    isDepositPaid,
                    row.getShippingProgress(),
                    row.getPreorderStatus(),
                    parseBoolean(row.getShipped()));
        }

        return StatusResolver.determineStandard(itemName, isPurchased, isDepositPaid, shippingStatus);
    }

    private ShippingStatus resolveShippingStatus(SheetRowDto row) {
        boolean isShipped = parseBoolean(row.getShipped());
        if (currentSourceType == GroupSourceType.PREORDER) {
            return StatusResolver.determinePreorderShipping(row.getShippingProgress(), row.getPreorderStatus(), isShipped);
        }
        return StatusResolver.determineStandardShipping(row.getShippingProgress(), parseBoolean(row.getArrived()), isShipped);
    }

    private Boolean resolveQueued(SheetRowDto row) {
        if (hasQueuedColumn) {
            return parseBoolean(row.getQueued());
        }
        if (hasLegacyQueuedColumn) {
            return parseBoolean(row.getLegacyQueued());
        }
        return null;
    }

    private boolean shouldSkipSheet(String sheetName) {
        if (sheetName != null && SETTINGS_SHEET_NAMES.contains(sheetName)) {
            return true;
        }
        if (sheetConfigs == null) {
            return false;
        }
        SheetSyncSheetConfig config = resolveSheetConfig(sheetName);
        return config == null || !config.isVisible();
    }

    private SheetSyncSheetConfig resolveSheetConfig(String sheetName) {
        if (sheetConfigs == null || sheetConfigs.isEmpty()) {
            return null;
        }
        String normalized = SheetNameNormalizer.normalize(sheetName);
        for (Map.Entry<String, SheetSyncSheetConfig> entry : sheetConfigs.entrySet()) {
            if (SheetNameNormalizer.isCompatible(
                    entry.getKey(),
                    normalized,
                    sheetNameMatchMaxCompareLength,
                    sheetNameMatchMinCompareLength)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private GroupSourceType resolveCurrentSourceType(SheetSyncSheetConfig config, boolean hasPreorderHeader) {
        boolean preorder = defaultSourceType == GroupSourceType.PREORDER;
        if (config != null && config.getPreorder() != null) {
            preorder = config.getPreorder();
        } else if (hasPreorderHeader) {
            preorder = true;
        }
        return preorder ? GroupSourceType.PREORDER : GroupSourceType.STANDARD;
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

    private boolean containsHeader(Set<String> headers, String headerName) {
        if (headers == null || headerName == null || headerName.isBlank()) {
            return false;
        }
        return headers.contains(headerName);
    }

    private static String resolveExcelHeader(String fieldName) {
        try {
            Field field = SheetRowDto.class.getDeclaredField(fieldName);
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty == null || excelProperty.value().length == 0) {
                return "";
            }
            return excelProperty.value()[0].trim();
        } catch (NoSuchFieldException e) {
            return "";
        }
    }

    private static Map<String, SheetSyncSheetConfig> toSheetConfigs(Set<String> visibleSheets) {
        if (visibleSheets == null || visibleSheets.isEmpty()) {
            return null;
        }
        Map<String, SheetSyncSheetConfig> configMap = new LinkedHashMap<>();
        for (String visibleSheet : visibleSheets) {
            configMap.put(visibleSheet, new SheetSyncSheetConfig(true, null));
        }
        return configMap;
    }

    private void flushItems(String phase) {
        try {
            sheetSyncWriter.saveItems(itemBuffer);
        } catch (Exception e) {
            log.warn("Sheet {} failed to save {} batch (rowCount={})", currentSheetName, phase, rowCount, e);
            for (OrderItem item : itemBuffer) {
                try {
                    sheetSyncWriter.saveItem(item);
                } catch (Exception ex) {
                    log.warn("Sheet {} failed to save single item (rowCount={}, item={})",
                            currentSheetName, rowCount, safeString(item.getItemName()), ex);
                }
            }
        }
    }
}

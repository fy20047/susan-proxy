package com.fy20047.susan.service;

import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncService {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("\\*(\\d+)");
    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "對帳", "定金50%", "尾款50%", "購買總額", "團友", "品項", "日幣原價", "已採購", "抵台", "出貨狀態"
    );

    private final OrderGroupRepository orderGroupRepository;

    public SheetSyncService(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    // CSV 同步主流程（無指定團名）
    @Transactional
    public void syncFromCsv(Path csvPath) {
        syncFromCsv(csvPath, null);
    }

    // CSV 同步主流程（指定團名）
    @Transactional
    public void syncFromCsv(Path csvPath, String explicitGroupName) {
        String groupName = resolveGroupName(csvPath, explicitGroupName);

        List<CSVRecord> records = readAllRecords(csvPath);
        int headerIndex = findHeaderIndex(records);
        if (headerIndex < 0) {
            throw new IllegalStateException("找不到欄位表頭，請確認 CSV 是否包含必要欄位");
        }

        Map<String, Integer> headerIndexMap = buildHeaderIndex(records.get(headerIndex));
        Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();

        for (int i = headerIndex + 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            String buyerNickname = getValue(record, headerIndexMap, "團友");
            String itemName = getValue(record, headerIndexMap, "品項");

            if (isBlank(buyerNickname) || isBlank(itemName)) {
                continue;
            }

            OrderGroup group = groupByBuyer.computeIfAbsent(buyerNickname, key -> {
                OrderGroup newGroup = new OrderGroup();
                newGroup.setBuyerNickname(key);
                newGroup.setGroupName(groupName);
                newGroup.setLastUpdated(LocalDateTime.now());
                return newGroup;
            });

            OrderItem item = new OrderItem();
            item.setOrderSn(getValue(record, headerIndexMap, "喊單序"));
            item.setBalanceDueDate(getValue(record, headerIndexMap, "尾款日"));
            item.setDepositPaidDate(getValue(record, headerIndexMap, "付定日"));
            item.setDepositAmount(parseInteger(getValue(record, headerIndexMap, "定金50%"), 0));
            item.setBalanceAmount(parseInteger(getValue(record, headerIndexMap, "尾款50%"), 0));
            item.setTotalAmount(parseInteger(getValue(record, headerIndexMap, "購買總額"), 0));
            item.setItemName(itemName);
            item.setQuantity(extractQuantity(itemName));
            item.setJpyPrice(parseInteger(getValue(record, headerIndexMap, "日幣原價"), null));

            boolean isReconciled = parseBoolean(getValue(record, headerIndexMap, "對帳"));
            boolean isPurchased = parseBoolean(getValue(record, headerIndexMap, "已採購"));
            boolean isArrived = parseBoolean(getValue(record, headerIndexMap, "抵台"));
            boolean isShipped = parseBoolean(getValue(record, headerIndexMap, "出貨狀態"));
            item.setItemStatus(determineStatus(isReconciled, isPurchased, isArrived, isShipped));

            group.addItem(item);
        }

        // 依團名整批刪除舊資料，再重建
        if (!isBlank(groupName)) {
            List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
            if (!existingGroups.isEmpty()) {
                orderGroupRepository.deleteAll(existingGroups);
            }
        }

        orderGroupRepository.saveAll(groupByBuyer.values());
    }

    // 解析 CSV 的 TRUE/FALSE 字串為布林值（允許大小寫、空白）
    public boolean parseBoolean(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        return TRUE_VALUES.contains(normalized);
    }

    // 將四個狀態欄位轉成單一 ItemStatus
    public ItemStatus determineStatus(boolean isReconciled, boolean isPurchased, boolean isArrived, boolean isShipped) {
        // 1. 最末端的狀態優先判斷（防呆）
        if (isShipped) {
            return ItemStatus.SHIPPED;
        }
        if (isArrived) {
            return ItemStatus.ARRIVED;
        }

        // 2. 十字交叉邏輯
        if (isPurchased && isReconciled) {
            return ItemStatus.IN_TRANSIT;
        } else if (isPurchased && !isReconciled) {
            return ItemStatus.PENDING_DEPOSIT;
        } else if (!isPurchased && isReconciled) {
            return ItemStatus.PENDING_PURCHASE;
        } else {
            return ItemStatus.REGISTERED;
        }
    }

    /**
     * 從原始品名字串中，萃取出購買數量。
     * 規則：尋找 "*數字" 的格式。若無，預設為 1。
     */
    public Integer extractQuantity(String rawItemName) {
        if (rawItemName == null || rawItemName.trim().isEmpty()) {
            return 1;
        }

        Matcher matcher = QUANTITY_PATTERN.matcher(rawItemName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 1;
    }

    // 讀取 CSV 全部列資料
    private List<CSVRecord> readAllRecords(Path csvPath) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            return parser.getRecords();
        } catch (IOException e) {
            throw new IllegalStateException("CSV 讀取失敗：" + csvPath, e);
        }
    }

    // 尋找真正的表頭列（可跳過公告/說明列）
    private int findHeaderIndex(List<CSVRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            Set<String> headerSet = new java.util.HashSet<>();
            for (String cell : record) {
                String normalized = normalizeHeaderName(cell);
                if (!normalized.isEmpty()) {
                    headerSet.add(normalized);
                }
            }
            if (headerSet.containsAll(REQUIRED_HEADERS)) {
                return i;
            }
        }
        return -1;
    }

    // 建立欄位名稱對應欄位索引
    private Map<String, Integer> buildHeaderIndex(CSVRecord headerRecord) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headerRecord.size(); i++) {
            String normalized = normalizeHeaderName(headerRecord.get(i));
            if (!normalized.isEmpty() && !indexMap.containsKey(normalized)) {
                indexMap.put(normalized, i);
            }
        }
        return indexMap;
    }

    // 依欄位名稱取得資料（若不存在回傳空字串）
    private String getValue(CSVRecord record, Map<String, Integer> headerIndexMap, String headerName) {
        Integer index = headerIndexMap.get(headerName);
        if (index == null || index < 0 || index >= record.size()) {
            return "";
        }
        return record.get(index);
    }

    // 處理表頭文字（去 BOM、修剪空白）
    private String normalizeHeaderName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '\uFEFF') {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    // 解析整數（允許空值與逗號）
    private Integer parseInteger(String rawValue, Integer defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        String normalized = rawValue.trim().replace(",", "");
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 若未指定團名，預設用檔名（不含副檔名）
    private String resolveGroupName(Path csvPath, String explicitGroupName) {
        if (!isBlank(explicitGroupName)) {
            return explicitGroupName.trim();
        }
        String fileName = csvPath.getFileName() == null ? "" : csvPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}

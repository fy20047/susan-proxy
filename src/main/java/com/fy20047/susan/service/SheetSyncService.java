package com.fy20047.susan.service;

import com.alibaba.excel.EasyExcel;
import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncService {

    private static final Logger log = LoggerFactory.getLogger(SheetSyncService.class);
    private static final List<String> SETTINGS_SHEET_NAMES = List.of("設定", "分頁");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("\\*(\\d+)");
    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "對帳", "定金80%", "尾款20%", "購買總額", "團友", "品項", "日幣原價", "已採購", "出貨狀態"
    );

    @Value("${app.google-sheet-url}")
    private String googleSheetUrl;

    private final OrderGroupRepository orderGroupRepository;

    public SheetSyncService(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @Transactional
    public void syncFromCsv(Path csvPath) {
        syncFromCsv(csvPath, null);
    }

    @Transactional
    public void syncFromCsv(Path csvPath, String explicitGroupName) {
        String groupName = resolveGroupName(csvPath, explicitGroupName);

        List<CSVRecord> records = readAllRecords(csvPath);
        int headerIndex = findHeaderIndex(records);
        if (headerIndex < 0) {
            throw new IllegalStateException("找不到欄位表頭，請確認 CSV 欄位名稱是否正確。");
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
            String orderSn = getValue(record, headerIndexMap, "順位");
            if (isBlank(orderSn)) {
                orderSn = getValue(record, headerIndexMap, "喊單序");
            }
            item.setOrderSn(orderSn);
            item.setQueued(parseBoolean(getValue(record, headerIndexMap, "是否排到")));
            item.setCheckedIn(parseBoolean(getValue(record, headerIndexMap, "報到")));
            item.setBalanceDueDate(getValue(record, headerIndexMap, "尾款日"));
            item.setDepositPaidDate(getValue(record, headerIndexMap, "付定日"));
            item.setDepositAmount(parseInteger(getValue(record, headerIndexMap, "定金80%"), 0));
            item.setBalanceAmount(parseInteger(getValue(record, headerIndexMap, "尾款20%"), 0));
            item.setTotalAmount(parseInteger(getValue(record, headerIndexMap, "購買總額"), 0));
            item.setItemName(itemName);
            item.setQuantity(parseInteger(getValue(record, headerIndexMap, "數量"), 1));
            // 如果未來賣家又把數量塞回品項，可以改回這行。
            // item.setQuantity(extractQuantity(itemName));
            item.setJpyPrice(parseInteger(getValue(record, headerIndexMap, "日幣原價"), null));

            boolean isReconciled = parseBoolean(getValue(record, headerIndexMap, "對帳"));
            boolean isPurchased = parseBoolean(getValue(record, headerIndexMap, "已採購"));
            boolean isArrived = parseBoolean(getValue(record, headerIndexMap, "抵台"));
            boolean isShipped = parseBoolean(getValue(record, headerIndexMap, "出貨狀態"));
            item.setItemStatus(determineStatus(isReconciled, isPurchased, isArrived, isShipped));

            group.addItem(item);
        }

        if (!isBlank(groupName)) {
            List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
            if (!existingGroups.isEmpty()) {
                orderGroupRepository.deleteAll(existingGroups);
            }
        }

        orderGroupRepository.saveAll(groupByBuyer.values());
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncFromGoogleSheetUrl() {
        if (isBlank(googleSheetUrl)) {
            return;
        }

        byte[] excelBytes = readExcelBytes(googleSheetUrl);
        logSheetNames(excelBytes);
        Map<String, Boolean> visibility = readSheetVisibility(excelBytes);
        Set<String> visibleSheets = null;
        if (visibility != null) {
            visibleSheets = new HashSet<>();
            for (Map.Entry<String, Boolean> entry : visibility.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    String normalized = SheetNameNormalizer.normalize(entry.getKey());
                    if (!normalized.isEmpty()) {
                        visibleSheets.add(normalized);
                    }
                }
            }
            log.info("設定分頁白名單(正規化後): {}", visibleSheets);
        }

        try (var inputStream = new ByteArrayInputStream(excelBytes)) {
            SheetRowListener listener = new SheetRowListener(orderGroupRepository, visibleSheets);
            EasyExcel.read(inputStream, SheetRowDto.class, listener).doReadAll();
            log.info("實際同步分頁(正規化後): {}", listener.getProcessedSheets());
            if (visibleSheets != null) {
                if (listener.getProcessedSheets().isEmpty()) {
                    log.warn("設定分頁有設定，但未同步到任何分頁，略過清除舊資料");
                } else {
                    deleteGroupsNotIn(visibleSheets);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Google Sheet Excel 讀取失敗：" + googleSheetUrl, e);
        }
    }

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

    public ItemStatus determineStatus(boolean isReconciled, boolean isPurchased, boolean isArrived, boolean isShipped) {
        if (isShipped) {
            return ItemStatus.SHIPPED;
        }
        if (isArrived) {
            return ItemStatus.ARRIVED;
        }

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

    private List<CSVRecord> readAllRecordsFromUrl(String csvUrl) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = new InputStreamReader(new URL(csvUrl).openStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            return parser.getRecords();
        } catch (IOException e) {
            throw new IllegalStateException("Google Sheet CSV 讀取失敗：" + csvUrl, e);
        }
    }

    private int findHeaderIndex(List<CSVRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            Set<String> headerSet = new HashSet<>();
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

    private String getValue(CSVRecord record, Map<String, Integer> headerIndexMap, String headerName) {
        Integer index = headerIndexMap.get(headerName);
        if (index == null || index < 0 || index >= record.size()) {
            return "";
        }
        return record.get(index);
    }

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

    private byte[] readExcelBytes(String url) {
        try (var inputStream = new URL(url).openStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Google Sheet Excel 下載失敗：" + url, e);
        }
    }

    private Map<String, Boolean> readSheetVisibility(byte[] excelBytes) {
        for (String sheetName : SETTINGS_SHEET_NAMES) {
            Map<String, Boolean> result = tryReadVisibility(excelBytes, sheetName);
            if (result != null) {
                return result;
            }
        }
        log.info("找不到設定/分頁分頁或讀取失敗，將同步所有分頁");
        return null;
    }

    private Map<String, Boolean> tryReadVisibility(byte[] excelBytes, String sheetName) {
        try (var inputStream = new ByteArrayInputStream(excelBytes)) {
            SheetVisibilityListener listener = new SheetVisibilityListener();
            EasyExcel.read(inputStream, SheetVisibilityRow.class, listener)
                    .sheet(sheetName)
                    .headRowNumber(1)
                    .doRead();
            Map<String, Boolean> result = listener.getVisibilityBySheet();
            if (result.isEmpty()) {
                log.info("設定分頁 {} 為空，將同步所有分頁", sheetName);
                return null;
            }
            log.info("使用設定分頁 {} 進行同步白名單", sheetName);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteGroupsNotIn(Set<String> visibleSheets) {
        if (visibleSheets == null) {
            return;
        }
        List<OrderGroup> allGroups = orderGroupRepository.findAll();
        if (allGroups.isEmpty()) {
            return;
        }

        List<OrderGroup> toDelete = new ArrayList<>();
        for (OrderGroup group : allGroups) {
            String groupName = group.getGroupName();
            String normalized = SheetNameNormalizer.normalize(groupName);
            if (normalized.isEmpty() || !visibleSheets.contains(normalized)) {
                toDelete.add(group);
            }
        }

        if (!toDelete.isEmpty()) {
            orderGroupRepository.deleteAll(toDelete);
        }
    }

    private void logSheetNames(byte[] excelBytes) {
        try (var inputStream = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            int count = workbook.getNumberOfSheets();
            if (count == 0) {
                log.warn("Excel 沒有任何分頁");
                return;
            }
            List<String> sheetNames = new ArrayList<>();
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = workbook.getSheetName(i);
                sheetNames.add(name);
                normalized.add(SheetNameNormalizer.normalize(name));
            }
            log.info("Excel 分頁名稱: {}", sheetNames);
            log.info("Excel 分頁名稱(正規化後): {}", normalized);
        } catch (Exception e) {
            log.warn("讀取 Excel 分頁名稱失敗", e);
        }
    }
}

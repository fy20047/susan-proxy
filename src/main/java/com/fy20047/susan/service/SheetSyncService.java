package com.fy20047.susan.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fy20047.susan.domain.GroupSourceType;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final List<String> SETTINGS_SHEET_NAMES = List.of("設定");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("\\*(\\d+)");
    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");
    private static final String BUYER_HEADER = resolveExcelHeader("buyerNickname");
    private static final String ITEM_NAME_HEADER = resolveExcelHeader("itemName");
    private static final String ORDER_RANK_HEADER = resolveExcelHeader("orderRank");
    private static final String ORDER_SN_HEADER = resolveExcelHeader("orderSn");
    private static final String QUEUED_HEADER = resolveExcelHeader("queued");
    private static final String LEGACY_QUEUED_HEADER = resolveExcelHeader("legacyQueued");
    private static final String CHECKED_IN_HEADER = resolveExcelHeader("checkedIn");
    private static final String BALANCE_DUE_DATE_HEADER = resolveExcelHeader("balanceDueDate");
    private static final String DEPOSIT_PAID_DATE_HEADER = resolveExcelHeader("depositPaidDate");
    private static final String CHECK_MARK_HEADER = resolveExcelHeader("checkMark");
    private static final String DEPOSIT_AMOUNT_HEADER = resolveExcelHeader("depositAmount");
    private static final String BALANCE_AMOUNT_HEADER = resolveExcelHeader("balanceAmount");
    private static final String TOTAL_AMOUNT_HEADER = resolveExcelHeader("totalAmount");
    private static final String QUANTITY_HEADER = resolveExcelHeader("quantity");
    private static final String JPY_PRICE_HEADER = resolveExcelHeader("jpyPrice");
    private static final String BONUS_HEADER = resolveExcelHeader("bonus");
    private static final String RECONCILED_HEADER = resolveExcelHeader("reconciled");
    private static final String PURCHASED_HEADER = resolveExcelHeader("purchased");
    private static final String ARRIVED_HEADER = resolveExcelHeader("arrived");
    private static final String SHIPPED_HEADER = resolveExcelHeader("shipped");
    private static final String PREORDER_STATUS_HEADER = resolveExcelHeader("preorderStatus");
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            RECONCILED_HEADER, DEPOSIT_AMOUNT_HEADER, BALANCE_AMOUNT_HEADER, TOTAL_AMOUNT_HEADER, BUYER_HEADER, ITEM_NAME_HEADER);

    @Value("${app.google-sheet-url:}")
    private String legacyGoogleSheetUrl;

    @Value("${app.preorder-google-sheet-url:}")
    private String preorderGoogleSheetUrl;

    @Value("${app.standard-google-sheet-url:}")
    private String standardGoogleSheetUrl;

    @Value("${app.log-sheet-names:false}")
    private boolean logSheetNamesEnabled;

    @Value("${app.sheet-sync.stream-by-buyer:true}")
    private boolean streamByBuyer;

    @Value("${app.sheet-sync.max-rows-warn:5000}")
    private int maxRowsWarn;

    @Value("${app.sheet-sync.item-batch-size:200}")
    private int itemBatchSize;

    @Value("${app.sheet-sync.sheet-name-match-max-compare-length:64}")
    private int sheetNameMatchMaxCompareLength;

    @Value("${app.sheet-sync.sheet-name-match-min-compare-length:16}")
    private int sheetNameMatchMinCompareLength;

    private final OrderGroupRepository orderGroupRepository;
    private final SheetSyncWriter sheetSyncWriter;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    public SheetSyncService(OrderGroupRepository orderGroupRepository, SheetSyncWriter sheetSyncWriter) {
        this.orderGroupRepository = orderGroupRepository;
        this.sheetSyncWriter = sheetSyncWriter;
    }

    @Transactional
    public void syncFromCsv(Path csvPath) {
        syncFromCsv(csvPath, null);
    }

    @Transactional
    public void syncFromCsv(Path csvPath, String explicitGroupName) {
        String groupName = resolveGroupName(csvPath, explicitGroupName);
        String sourceKey = "csv";

        List<CSVRecord> records = readAllRecords(csvPath);
        int headerIndex = findHeaderIndex(records);
        if (headerIndex < 0) {
            throw new IllegalStateException("CSV did not contain a supported header row: " + csvPath);
        }

        Map<String, Integer> headerIndexMap = buildHeaderIndex(records.get(headerIndex));
        boolean hasQueuedColumn = containsHeader(headerIndexMap, QUEUED_HEADER);
        boolean hasLegacyQueuedColumn = containsHeader(headerIndexMap, LEGACY_QUEUED_HEADER);
        boolean hasPreorderStatusColumn = containsHeader(headerIndexMap, PREORDER_STATUS_HEADER);
        Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();

        for (int i = headerIndex + 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            String buyerNickname = getValue(record, headerIndexMap, BUYER_HEADER);
            String itemName = getValue(record, headerIndexMap, ITEM_NAME_HEADER);

            if (isBlank(buyerNickname) || isBlank(itemName)) {
                continue;
            }

            GroupSourceType sourceType = hasPreorderStatusColumn ? GroupSourceType.PREORDER : GroupSourceType.STANDARD;
            OrderGroup group = groupByBuyer.computeIfAbsent(buyerNickname, key -> {
                OrderGroup newGroup = new OrderGroup();
                newGroup.setBuyerNickname(key);
                newGroup.setGroupName(groupName);
                newGroup.setSourceKey(sourceKey);
                newGroup.setSourceType(sourceType);
                newGroup.setLastUpdated(LocalDateTime.now());
                return newGroup;
            });

            Integer bonus = parseInteger(getValue(record, headerIndexMap, BONUS_HEADER), null);
            if (bonus != null) {
                int current = group.getBonusCount() == null ? 0 : group.getBonusCount();
                if (bonus > current) {
                    group.setBonusCount(bonus);
                }
            }

            OrderItem item = new OrderItem();
            String orderSn = getValue(record, headerIndexMap, ORDER_RANK_HEADER);
            if (isBlank(orderSn)) {
                orderSn = getValue(record, headerIndexMap, ORDER_SN_HEADER);
            }
            item.setOrderSn(orderSn);
            item.setQueued(resolveQueued(record, headerIndexMap, hasQueuedColumn, hasLegacyQueuedColumn));
            item.setCheckedIn(parseBoolean(getValue(record, headerIndexMap, CHECKED_IN_HEADER)));
            item.setBalanceDueDate(getValue(record, headerIndexMap, BALANCE_DUE_DATE_HEADER));
            String depositPaidDate = getValue(record, headerIndexMap, DEPOSIT_PAID_DATE_HEADER);
            item.setDepositPaidDate(depositPaidDate);
            item.setDepositReconciled(parseBoolean(getValue(record, headerIndexMap, RECONCILED_HEADER)));
            boolean isPurchased = parseBoolean(getValue(record, headerIndexMap, PURCHASED_HEADER));
            item.setPurchased(isPurchased);
            String checkMark = getValue(record, headerIndexMap, CHECK_MARK_HEADER);
            if (isBlank(checkMark) && !isBlank(depositPaidDate)) {
                checkMark = depositPaidDate;
            }
            item.setCheckMark(checkMark);
            item.setDepositAmount(parseInteger(getValue(record, headerIndexMap, DEPOSIT_AMOUNT_HEADER), 0));
            item.setBalanceAmount(parseInteger(getValue(record, headerIndexMap, BALANCE_AMOUNT_HEADER), 0));
            item.setTotalAmount(parseInteger(getValue(record, headerIndexMap, TOTAL_AMOUNT_HEADER), 0));
            item.setItemName(itemName);
            item.setQuantity(parseInteger(getValue(record, headerIndexMap, QUANTITY_HEADER), 1));
            item.setJpyPrice(parseInteger(getValue(record, headerIndexMap, JPY_PRICE_HEADER), null));

            if (hasPreorderStatusColumn) {
                boolean isShipped = parseBoolean(getValue(record, headerIndexMap, SHIPPED_HEADER));
                item.setItemStatus(StatusResolver.determinePreorder(
                        getValue(record, headerIndexMap, PREORDER_STATUS_HEADER),
                        isShipped));
            } else {
                boolean isReconciled = parseBoolean(getValue(record, headerIndexMap, RECONCILED_HEADER));
                boolean isArrived = parseBoolean(getValue(record, headerIndexMap, ARRIVED_HEADER));
                boolean isShipped = parseBoolean(getValue(record, headerIndexMap, SHIPPED_HEADER));
                item.setItemStatus(StatusResolver.determineLegacy(isReconciled, isPurchased, isArrived, isShipped));
            }

            group.addItem(item);
        }

        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupNameAndSourceKeyIncludingLegacy(groupName, sourceKey);
        orderGroupRepository.saveAll(groupByBuyer.values());
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void syncFromGoogleSheetUrl() {
        syncFromGoogleSheets();
    }

    public void syncFromGoogleSheets() {
        List<SyncSource> sources = resolveSyncSources();
        if (sources.isEmpty()) {
            return;
        }
        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Sheet sync is already running.");
            return;
        }

        int successCount = 0;
        Exception firstFailure = null;
        try {
            for (SyncSource source : sources) {
                try {
                    syncSource(source);
                    successCount += 1;
                } catch (Exception e) {
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                    log.error("Failed to sync source {} from {}", source.sourceKey(), source.url(), e);
                }
            }

            if (successCount == 0 && firstFailure != null) {
                throw new IllegalStateException("All Google Sheet sync sources failed.", firstFailure);
            }
        } finally {
            syncInProgress.set(false);
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

    private void syncSource(SyncSource source) {
        byte[] excelBytes = readExcelBytes(source.url());
        if (logSheetNamesEnabled) {
            logSheetNames(excelBytes);
        }

        Map<String, SheetSyncSheetConfig> sheetConfigs = readSheetConfigs(excelBytes);
        Set<String> visibleSheets = extractVisibleSheets(sheetConfigs);

        try (var inputStream = new ByteArrayInputStream(excelBytes)) {
            SheetRowListener listener = new SheetRowListener(
                    sheetSyncWriter,
                    source.sourceKey(),
                    source.defaultSourceType(),
                    sheetConfigs,
                    streamByBuyer,
                    maxRowsWarn,
                    itemBatchSize,
                    sheetNameMatchMaxCompareLength,
                    sheetNameMatchMinCompareLength);
            EasyExcel.read(inputStream, SheetRowDto.class, listener).doReadAll();
            log.info("Synced source {} sheets={}", source.sourceKey(), listener.getProcessedSheets());
            if (visibleSheets != null) {
                if (listener.getProcessedSheets().isEmpty()) {
                    log.warn("Source {} had visible sheets configured but none were processed.", source.sourceKey());
                } else {
                    deleteGroupsNotIn(source.sourceKey(), visibleSheets);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read workbook stream for source " + source.sourceKey(), e);
        }
    }

    private List<SyncSource> resolveSyncSources() {
        List<SyncSource> sources = new ArrayList<>();
        if (!isBlank(preorderGoogleSheetUrl)) {
            sources.add(new SyncSource("preorder", preorderGoogleSheetUrl.trim(), GroupSourceType.PREORDER));
        }
        String standardUrl = !isBlank(standardGoogleSheetUrl) ? standardGoogleSheetUrl : legacyGoogleSheetUrl;
        if (!isBlank(standardUrl)) {
            sources.add(new SyncSource("standard", standardUrl.trim(), GroupSourceType.STANDARD));
        }
        return sources;
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
            throw new IllegalStateException("Failed to read CSV: " + csvPath, e);
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

    private boolean containsHeader(Map<String, Integer> headerIndexMap, String headerName) {
        if (headerIndexMap == null || headerName == null || headerName.isBlank()) {
            return false;
        }
        return headerIndexMap.containsKey(headerName);
    }

    private Boolean resolveQueued(
            CSVRecord record,
            Map<String, Integer> headerIndexMap,
            boolean hasQueuedColumn,
            boolean hasLegacyQueuedColumn
    ) {
        if (hasQueuedColumn) {
            return parseBoolean(getValue(record, headerIndexMap, QUEUED_HEADER));
        }
        if (hasLegacyQueuedColumn) {
            return parseBoolean(getValue(record, headerIndexMap, LEGACY_QUEUED_HEADER));
        }
        return null;
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
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            try (var inputStream = connection.getInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download Google Sheet Excel: " + url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Map<String, SheetSyncSheetConfig> readSheetConfigs(byte[] excelBytes) {
        for (String sheetName : SETTINGS_SHEET_NAMES) {
            Map<String, SheetSyncSheetConfig> result = tryReadSheetConfig(excelBytes, sheetName);
            if (result != null) {
                return result;
            }
        }
        log.info("No settings sheet was found; syncing all compatible sheets.");
        return null;
    }

    private Map<String, SheetSyncSheetConfig> tryReadSheetConfig(byte[] excelBytes, String sheetName) {
        try (var inputStream = new ByteArrayInputStream(excelBytes)) {
            SheetVisibilityListener listener = new SheetVisibilityListener();
            EasyExcel.read(inputStream, SheetVisibilityRow.class, listener)
                    .sheet(sheetName)
                    .headRowNumber(1)
                    .doRead();
            Map<String, SheetSyncSheetConfig> result = listener.getConfigBySheet();
            if (result.isEmpty()) {
                log.info("Settings sheet {} was present but empty.", sheetName);
                return null;
            }
            log.info("Loaded settings sheet {} entries={}", sheetName, result.keySet());
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> extractVisibleSheets(Map<String, SheetSyncSheetConfig> sheetConfigs) {
        if (sheetConfigs == null || sheetConfigs.isEmpty()) {
            return null;
        }
        Set<String> visibleSheets = new HashSet<>();
        for (Map.Entry<String, SheetSyncSheetConfig> entry : sheetConfigs.entrySet()) {
            if (entry.getValue().isVisible()) {
                visibleSheets.add(entry.getKey());
            }
        }
        return visibleSheets;
    }

    private void deleteGroupsNotIn(String sourceKey, Set<String> visibleSheets) {
        if (visibleSheets == null) {
            return;
        }
        List<OrderGroup> allGroups = orderGroupRepository.findBySourceKey(sourceKey);
        if (allGroups.isEmpty()) {
            return;
        }

        List<OrderGroup> toDelete = new ArrayList<>();
        for (OrderGroup group : allGroups) {
            String groupName = group.getGroupName();
            String normalized = SheetNameNormalizer.normalize(groupName);
            if (normalized.isEmpty() || !isInVisibleSheets(visibleSheets, normalized)) {
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
                log.warn("Excel workbook contained no sheets.");
                return;
            }
            List<String> sheetNames = new ArrayList<>();
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = workbook.getSheetName(i);
                sheetNames.add(name);
                normalized.add(SheetNameNormalizer.normalize(name));
            }
            log.info("Excel sheet names: {}", sheetNames);
            log.info("Excel normalized sheet names: {}", normalized);
        } catch (Exception e) {
            log.warn("Failed to inspect workbook sheet names.", e);
        }
    }

    private boolean isInVisibleSheets(Set<String> visibleSheets, String normalizedSheetName) {
        if (visibleSheets == null || visibleSheets.isEmpty()) {
            return false;
        }
        for (String configuredSheet : visibleSheets) {
            if (SheetNameNormalizer.isCompatible(
                    configuredSheet,
                    normalizedSheetName,
                    sheetNameMatchMaxCompareLength,
                    sheetNameMatchMinCompareLength)) {
                return true;
            }
        }
        return false;
    }

    private record SyncSource(String sourceKey, String url, GroupSourceType defaultSourceType) {
    }
}

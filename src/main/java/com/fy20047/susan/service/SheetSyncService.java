package com.fy20047.susan.service;

import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import com.fy20047.susan.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

    public SheetSyncService(OrderGroupRepository orderGroupRepository, OrderItemRepository orderItemRepository) {
        // 這邊是建構子 (Constructor)，Spring Boot 啟動時會自動把兩個 Repository 交給這個 Service 使用依賴注入 (DI)
        this.orderGroupRepository = orderGroupRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // 匯入沒指定團名的 CSV
    @Transactional
    public void syncFromCsv(Path csvPath) {
        // 這邊會留 null，之後讓 resolveGroupName 處理
        syncFromCsv(csvPath, null);
    }

    // 匯入指定團名的 CSV
    @Transactional
    public void syncFromCsv(Path csvPath, String explicitGroupName) {
        // 如果 explicitGroupName 是空的，就去拿檔案名稱當作團名
        String groupName = resolveGroupName(csvPath, explicitGroupName);

        // 把整份 CSV 檔案的每一行讀進來，變成一個大Array (records)
        List<CSVRecord> records = readAllRecords(csvPath);
        // 找出標題列在第幾行
        int headerIndex = findHeaderIndex(records);
        if (headerIndex < 0) {
            // 如果文件都找不到「對帳、團友、品項...」這些字，直接報錯
            throw new IllegalStateException("找不到欄位表頭，請確認 CSV 是否包含必要欄位");
        }

        // 根據標題列，做出一張欄位名稱 vs 格子位置的對照表 (例如："團友" 在第 0 格)
        Map<String, Integer> headerIndexMap = buildHeaderIndex(records.get(headerIndex));
        // 準備一張大桌子 (LinkedHashMap)，用來把屬於同一個買家(Key)的 OrderGroup 放在一起
        Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();

        // 從標題列的下一行 (headerIndex + 1)開始，一行一行往下讀取買家資料
        for (int i = headerIndex + 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            // 透過 getValue，拿出該行的買家名稱和商品名稱
            String buyerNickname = getValue(record, headerIndexMap, "團友");
            String itemName = getValue(record, headerIndexMap, "品項");

            // 如果沒寫是誰買的，或者沒寫買什麼，直接跳過看下一行
            if (isBlank(buyerNickname) || isBlank(itemName)) {
                continue;
            }

            // 尋找或創建 OrderGroup (主檔)，系統會先去 groupByBuyer) 上找買家的名字
            OrderGroup group = groupByBuyer.computeIfAbsent(buyerNickname, key -> {
                // 如果還沒有這個買家的 OrderGroup，就 new 給他
                OrderGroup newGroup = new OrderGroup();
                newGroup.setBuyerNickname(key);       // 寫上買家名字
                newGroup.setGroupName(groupName);     // 寫上團名
                newGroup.setLastUpdated(LocalDateTime.now()); // 記錄現在的更新時間
                return newGroup;
            });

            // 創建 OrderItem 並塞入資料
            OrderItem item = new OrderItem();
            item.setOrderSn(getValue(record, headerIndexMap, "喊單序")); // 存入訂單編號
            item.setBalanceDueDate(getValue(record, headerIndexMap, "尾款日"));
            item.setDepositPaidDate(getValue(record, headerIndexMap, "付定日"));

            // 套用寫好的 parseInteger 數字
            item.setDepositAmount(parseInteger(getValue(record, headerIndexMap, "定金50%"), 0));
            item.setBalanceAmount(parseInteger(getValue(record, headerIndexMap, "尾款50%"), 0));
            item.setTotalAmount(parseInteger(getValue(record, headerIndexMap, "購買總額"), 0));

            item.setItemName(itemName); // 存入商品名
            item.setQuantity(extractQuantity(itemName)); // 把 "*3" 轉成數字 3
            item.setJpyPrice(parseInteger(getValue(record, headerIndexMap, "日幣原價"), null));

            // 讀取 4 個打勾狀態 (轉成 true/false)
            boolean isReconciled = parseBoolean(getValue(record, headerIndexMap, "對帳"));
            boolean isPurchased = parseBoolean(getValue(record, headerIndexMap, "已採購"));
            boolean isArrived = parseBoolean(getValue(record, headerIndexMap, "抵台"));
            boolean isShipped = parseBoolean(getValue(record, headerIndexMap, "出貨狀態"));

            // 根據這 4 個 true/false 判定出唯一狀態，存入 OrderItem
            item.setItemStatus(determineStatus(isReconciled, isPurchased, isArrived, isShipped));

            // 把做好的item，連到該買家的 group 上
            // 這個 addItem 會同時自動綁好外鍵 (order_group_id)
            group.addItem(item);
        }

        // 如果有團名 (例如 "10月東京團")
        if (!isBlank(groupName)) {
            // 先去資料庫把以前存過的 "10月東京團" 所有 OrderGroup 都找出來
            List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
            if (!existingGroups.isEmpty()) {
                // 把舊資料全部刪除，確保等等存入的新資料不會重複
                orderGroupRepository.deleteAll(existingGroups);
            }
        }

        // 重新儲存進去 DB
        orderGroupRepository.saveAll(groupByBuyer.values());
    }

    // 把 CSV 裡的輸入（V、v、TRUE、1 或是空白），統一變成 TRUE/FALSE
    public boolean parseBoolean(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        // 清洗資料
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        // 如果符合 TRUE_VALUES 的內容就算 TRUE
        return TRUE_VALUES.contains(normalized);
    }

    // 將四個狀態欄位轉成單一 ItemStatus
    public ItemStatus determineStatus(boolean isReconciled, boolean isPurchased, boolean isArrived, boolean isShipped) {
        // 按照狀態優先判斷
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


    // 從原始品名字串中，抓出購買數量，尋找 "*數字" 的格式，若無則設為 1
    public Integer extractQuantity(String rawItemName) {

        // 如果品名是 null 或者是全空白，算為 1 件，但通常不會發生
        if (rawItemName == null || rawItemName.trim().isEmpty()) {
            return 1;
        }

        // 把品名字串交給前面定義的正則式 QUANTITY_PATTERN 去掃描來產生一個 matcher
        Matcher matcher = QUANTITY_PATTERN.matcher(rawItemName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        // 沒找到 "*數字" 的格式，則為 1 件
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

    // 尋找真正的表頭列（避免抓到公告/說明列）
    // records 裡面裝著這個 CSV 檔的「每一列」資料
    private int findHeaderIndex(List<CSVRecord> records) {
        // 從第 0 列（最上面第一行）開始，往下掃描每一行
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i); // 拿出當前這一列

            // 準備一個空籃子 (Set)，用來裝這一列裡面「所有的文字」
            Set<String> headerSet = new java.util.HashSet<>();

            // 把這一列的每一個格子(cell)都看過一遍
            for (String cell : record) {
                String normalized = normalizeHeaderName(cell); // 把文字清乾淨（去掉空白、去 BOM）
                if (!normalized.isEmpty()) {
                    headerSet.add(normalized); // 把乾淨的文字丟進籃子裡
                }
            }
            // 3. 檢查這個籃子裡是不是完全包含了規定的必備欄位
            // REQUIRED_HEADERS 就是你寫在最上面的 ("對帳", "團友", "品項"...)
            if (headerSet.containsAll(REQUIRED_HEADERS)) {
                return i; // 如果全部都有則回傳這行的行號
            }
        }
        return -1; // 檔案格式不對
    }

    // 綁定標題名稱和它所在的格子位置
    // 傳入已經確定是表頭的那一整列資料 (headerRecord)
    private Map<String, Integer> buildHeaderIndex(CSVRecord headerRecord) {
        // 準備一個空的對照表，格式會像是 ["團友" -> 0, "品項" -> 1]
        Map<String, Integer> indexMap = new HashMap<>();
        // 從第 0 格開始，一格一格往右邊讀取表頭
        for (int i = 0; i < headerRecord.size(); i++) {
            // 把這一格的文字拿出來並用 normalizeHeaderName 做處理
            String normalized = normalizeHeaderName(headerRecord.get(i));
            // 如果這格不是空的，而且我們的對照表裡還沒收錄過這個標題
            // 這樣可以避免賣家不小心寫了兩個一模一樣的欄位名稱導致出問題
            if (!normalized.isEmpty() && !indexMap.containsKey(normalized)) {
                // 把標題名稱跟它的位置號碼 (i) 寫進對照表裡
                indexMap.put(normalized, i);
            }
        }
        return indexMap;
    }

    // 這邊會傳入現在讀到的這一列資料 (record)、剛剛做好的對照表以及想找的欄位名稱
    // 依欄位名稱取得資料（若不存在回傳空字串）
    private String getValue(CSVRecord record, Map<String, Integer> headerIndexMap, String headerName) {
        // 去對照表查這個欄位在哪一格
        Integer index = headerIndexMap.get(headerName);
        // 如果對照表裡沒有該欄位，或是這個位置超出了這列的總長度
        if (index == null || index < 0 || index >= record.size()) {
            return ""; // 安全回傳空字串，不讓程式崩潰
        }
        return record.get(index); // 從這一列資料中把那一格的內容抽出來給使用
    }

    // 標題清洗，處理表頭文字（去 BOM、修剪空白）
    private String normalizeHeaderName(String raw) {
        if (raw == null) {
            return "";
        }
        // 先把文字前後的空白切掉
        String trimmed = raw.trim();
        // 如果文字不是空的，而且第一個字元剛好是 '\uFEFF' (看不見的 BOM 標記)
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '\uFEFF') {
            // substring(1) 代表從第 1 個字元開始截取 (也就是把第 0 個那個隱形字元砍掉)
            trimmed = trimmed.substring(1);
        }
        return trimmed; // 回傳乾淨的標題
    }

    // 解析整數（允許空值與逗號）
    private Integer parseInteger(String rawValue, Integer defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        // 把字串裡面的逗號刪掉
        String normalized = rawValue.trim().replace(",", "");
        if (normalized.isEmpty()) { // 如果洗完發現沒東西，給預設值
            return defaultValue;
        }
        try {
            return Integer.parseInt(normalized); // 嘗試把乾淨的 "1500" 轉成數學整數 1500
        } catch (NumberFormatException e) { // 如果格子內不是數字的中文字，回傳預設值
            return defaultValue;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty(); // 如果是 null，或者把空白切掉後是空的，就回傳 true (代表這是空字串)
    }

    // 若未指定團名，預設用檔名（不含副檔名）
    private String resolveGroupName(Path csvPath, String explicitGroupName) {
        if (!isBlank(explicitGroupName)) {
            return explicitGroupName.trim();
        }
        String fileName = csvPath.getFileName() == null ? "" : csvPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.'); // 找出檔名中最後一個點的位置 (用來找副檔名)
        if (dotIndex > 0) { // 如果有找到點 代表有副檔名
            return fileName.substring(0, dotIndex); // substring(0, dotIndex) 代表從最前面切到點之前
        }
        return fileName; // // 如果檔名沒有點就直接用原檔名
    }
}

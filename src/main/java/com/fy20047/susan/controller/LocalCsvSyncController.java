package com.fy20047.susan.controller;

import com.fy20047.susan.service.SheetSyncService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 加上@RestController後回傳的所有資料都會被自動轉換成前端看得懂的 JSON 格式
@RestController


// dev 代表這是開發測試用的 API
@RequestMapping("/api/dev")
public class LocalCsvSyncController {

    // 設定預設測試用的 CSV 檔案路徑
    // 發送請求時沒有特別指定要讀哪個檔案，會自動去專案資料夾底下找這份檔案
    private static final String DEFAULT_SAMPLE_PATH =
            "src/main/resources/sample-data/13對帳用-0107-10東京連線.csv";

    private final SheetSyncService sheetSyncService;

    // DI
    public LocalCsvSyncController(SheetSyncService sheetSyncService) {
        this.sheetSyncService = sheetSyncService;
    }

    // 規定 API 的存取方法與子網址
    // API 的完整網址就是： POST http://localhost:8080/api/dev/sync-csv
    @PostMapping("/sync-csv")
    public Map<String, Object> syncCsv(
            // @RequestParam 接收前端傳來的參數 (file=某個路徑)
            @RequestParam(value = "file", required = false) String file,
            @RequestParam(value = "groupName", required = false) String groupName
    ) {

        // 判斷要讀取哪個檔案：
        // 如果前端沒有傳 file 參數過來或傳了空字串，就用上面定義好的 DEFAULT_SAMPLE_PATH
        // 否則就使用前端傳來的那個路徑，然後把它轉換成 Java 的 Path 物件
        Path csvPath = Path.of(file == null || file.trim().isEmpty() ? DEFAULT_SAMPLE_PATH : file.trim());

        // 🚀 【核心動作：呼叫理貨員上工！】
        // 把決定好的檔案路徑，以及前端指定的團名 (可能也是空的)，交給 syncFromCsv 執行解析跟存檔
        sheetSyncService.syncFromCsv(csvPath, groupName);

        // 以下是準備要回傳給前端的結果

        // 準備一個 Map ( JSON 物件)
        Map<String, Object> result = new LinkedHashMap<>();

        // 放入執行成功的狀態文字
        result.put("status", "ok");
        // 放入這次實際讀取的檔案路徑，讓呼叫 API 的人知道系統到底讀了哪份檔案
        result.put("path", csvPath.toString());
        // 放入這次使用的團名
        result.put("groupName", groupName);

        // 將這個 Map 回傳
        return result;
    }
}
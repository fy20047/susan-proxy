package com.fy20047.susan.controller;

import com.fy20047.susan.service.SheetSyncService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class LocalCsvSyncController {

    private static final String DEFAULT_SAMPLE_PATH =
            "src/main/resources/sample-data/13對帳用-0107-10東京連線.csv";

    private final SheetSyncService sheetSyncService;

    public LocalCsvSyncController(SheetSyncService sheetSyncService) {
        this.sheetSyncService = sheetSyncService;
    }

    @PostMapping("/sync-csv")
    public Map<String, Object> syncCsv(
            @RequestParam(value = "file", required = false) String file,
            @RequestParam(value = "groupName", required = false) String groupName
    ) {
        Path csvPath = Path.of(file == null || file.trim().isEmpty() ? DEFAULT_SAMPLE_PATH : file.trim());
        sheetSyncService.syncFromCsv(csvPath, groupName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("path", csvPath.toString());
        result.put("groupName", groupName);
        return result;
    }

    @PostMapping("/sync-sheet")
    public Map<String, Object> syncGoogleSheet() {
        sheetSyncService.syncFromGoogleSheetUrl();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("source", "googleSheetUrl");
        return result;
    }
}

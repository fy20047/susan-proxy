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
    private static final Path ALLOWED_BASE = Path.of("src", "main", "resources", "sample-data")
            .toAbsolutePath()
            .normalize();

    private final SheetSyncService sheetSyncService;

    public LocalCsvSyncController(SheetSyncService sheetSyncService) {
        this.sheetSyncService = sheetSyncService;
    }

    @PostMapping("/sync-csv")
    public Map<String, Object> syncCsv(
            @RequestParam(value = "file", required = false) String file,
            @RequestParam(value = "groupName", required = false) String groupName
    ) {
        Path csvPath = resolveCsvPath(file);
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

    private Path resolveCsvPath(String file) {
        if (file == null || file.trim().isEmpty()) {
            return Path.of(DEFAULT_SAMPLE_PATH).toAbsolutePath().normalize();
        }

        Path rawPath = Path.of(file.trim());
        Path resolved = rawPath.isAbsolute() ? rawPath : ALLOWED_BASE.resolve(rawPath);
        resolved = resolved.toAbsolutePath().normalize();
        if (!resolved.startsWith(ALLOWED_BASE)) {
            throw new IllegalArgumentException("Disallowed file path.");
        }
        return resolved;
    }
}

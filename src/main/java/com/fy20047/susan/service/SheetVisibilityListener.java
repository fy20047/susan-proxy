package com.fy20047.susan.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SheetVisibilityListener extends AnalysisEventListener<SheetVisibilityRow> {

    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");
    private final Map<String, SheetSyncSheetConfig> configBySheet = new LinkedHashMap<>();

    @Override
    public void invoke(SheetVisibilityRow row, AnalysisContext context) {
        if (row == null) {
            return;
        }

        String sheetName = SheetNameNormalizer.normalize(row.getSheetName());
        if (sheetName.isEmpty()) {
            return;
        }

        configBySheet.put(sheetName, new SheetSyncSheetConfig(
                parseBoolean(row.getVisible()),
                parsePreorder(row)));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // no-op
    }

    public Map<String, SheetSyncSheetConfig> getConfigBySheet() {
        return configBySheet;
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

    private Boolean parsePreorder(SheetVisibilityRow row) {
        String rawValue = firstNonBlank(row.getPreorder(), row.getMode());
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("受注".equals(trimmed)) {
            return true;
        }
        if ("一般".equals(trimmed)) {
            return false;
        }
        return parseBoolean(trimmed);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}

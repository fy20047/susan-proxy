package com.fy20047.susan.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SheetVisibilityListener extends AnalysisEventListener<SheetVisibilityRow> {

    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");

    private final Map<String, Boolean> visibilityBySheet = new LinkedHashMap<>();

    @Override
    public void invoke(SheetVisibilityRow row, AnalysisContext context) {
        if (row == null) {
            return;
        }

        String sheetName = SheetNameNormalizer.normalize(row.getSheetName());
        if (sheetName.isEmpty()) {
            return;
        }

        visibilityBySheet.put(sheetName, parseBoolean(row.getVisible()));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 讀取完成後不需額外處理
    }

    public Map<String, Boolean> getVisibilityBySheet() {
        return visibilityBySheet;
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

}

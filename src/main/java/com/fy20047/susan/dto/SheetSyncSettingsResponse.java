package com.fy20047.susan.dto;

import java.util.List;

public record SheetSyncSettingsResponse(
        boolean autoSyncEnabled,
        List<SheetSyncSourceDto> sources
) {
}

package com.fy20047.susan.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSyncResponse(
        String status,
        String source,
        LocalDateTime syncedAt,
        int totalSources,
        int syncedSources,
        int failedSources,
        List<AdminSyncWarningDto> warnings
) {
}

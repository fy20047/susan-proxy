package com.fy20047.susan.dto;

import com.fy20047.susan.domain.GroupSourceType;
import java.time.LocalDateTime;

public record SheetSyncSourceDto(
        Long id,
        String displayName,
        String sheetUrl,
        GroupSourceType defaultSourceType,
        LocalDateTime lastSyncedAt
) {
}

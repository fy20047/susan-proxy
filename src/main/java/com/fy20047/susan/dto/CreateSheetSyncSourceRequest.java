package com.fy20047.susan.dto;

import com.fy20047.susan.domain.GroupSourceType;

public record CreateSheetSyncSourceRequest(
        String displayName,
        String sheetUrl,
        GroupSourceType defaultSourceType
) {
}

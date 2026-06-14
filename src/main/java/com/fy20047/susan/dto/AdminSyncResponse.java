package com.fy20047.susan.dto;

import java.time.LocalDateTime;

public record AdminSyncResponse(String status, String source, LocalDateTime syncedAt) {
}

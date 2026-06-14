package com.fy20047.susan.dto;

public record AdminSyncWarningDto(
        String source,
        String sheetName,
        int rowNumber,
        String buyerNickname,
        String itemName,
        String message
) {
}

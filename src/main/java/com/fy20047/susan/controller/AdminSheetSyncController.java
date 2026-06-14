package com.fy20047.susan.controller;

import com.fy20047.susan.dto.AdminSyncResponse;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.service.AdminAuthService;
import com.fy20047.susan.service.SheetSyncService;
import com.fy20047.susan.service.SheetSyncService.SyncRunResult;
import com.fy20047.susan.service.SheetSyncService.SyncRunStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sheet-sync")
public class AdminSheetSyncController {

    private final AdminAuthService adminAuthService;
    private final SheetSyncService sheetSyncService;

    public AdminSheetSyncController(AdminAuthService adminAuthService, SheetSyncService sheetSyncService) {
        this.adminAuthService = adminAuthService;
        this.sheetSyncService = sheetSyncService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminSyncResponse>> createSheetSync(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "登入已逾時，請重新登入。"));
        }

        SyncRunResult result = sheetSyncService.syncFromGoogleSheetUrl();
        if (result.status() == SyncRunStatus.ALREADY_RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("SYNC_IN_PROGRESS", "Google Sheet 正在同步中，請稍後再試。"));
        }
        if (result.status() == SyncRunStatus.NO_SOURCES) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SYNC_SOURCE_NOT_CONFIGURED", "尚未設定 Google Sheet URL。"));
        }

        AdminSyncResponse response = new AdminSyncResponse("synced", "googleSheet", result.syncedAt());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.fy20047.susan.controller;

import com.fy20047.susan.domain.SheetSyncSource;
import com.fy20047.susan.dto.AdminSyncResponse;
import com.fy20047.susan.dto.AdminSyncWarningDto;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.dto.CreateSheetSyncSourceRequest;
import com.fy20047.susan.dto.SheetSyncSettingsResponse;
import com.fy20047.susan.dto.SheetSyncSourceDto;
import com.fy20047.susan.dto.UpdateAutoSyncRequest;
import com.fy20047.susan.service.AdminAuthService;
import com.fy20047.susan.service.SheetSyncService;
import com.fy20047.susan.service.SheetSyncService.SyncRunResult;
import com.fy20047.susan.service.SheetSyncService.SyncRunStatus;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping
    public ResponseEntity<ApiResponse<SheetSyncSettingsResponse>> getSheetSyncSettings(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }

        return ResponseEntity.ok(ApiResponse.success(buildSettingsResponse()));
    }

    @PutMapping("/settings/auto-sync")
    public ResponseEntity<ApiResponse<SheetSyncSettingsResponse>> updateAutoSync(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) UpdateAutoSyncRequest request
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "缺少自動同步設定。"));
        }

        sheetSyncService.setAutoSyncEnabled(request.autoSyncEnabled());
        return ResponseEntity.ok(ApiResponse.success(buildSettingsResponse()));
    }

    @PostMapping("/sources")
    public ResponseEntity<ApiResponse<SheetSyncSourceDto>> createSource(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CreateSheetSyncSourceRequest request
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "缺少表單連結資料。"));
        }

        try {
            SheetSyncSource source = sheetSyncService.createSyncSource(
                    request.displayName(),
                    request.sheetUrl(),
                    request.defaultSourceType());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(toSourceDto(source)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
        }
    }

    @DeleteMapping("/sources/{sourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteSource(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("sourceId") Long sourceId
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }

        boolean deleted = sheetSyncService.deleteSyncSource(sourceId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無指定表單連結。"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true, "sourceId", sourceId)));
    }

    @PostMapping("/sources/{sourceId}/sync")
    public ResponseEntity<ApiResponse<AdminSyncResponse>> syncSource(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("sourceId") Long sourceId
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }

        return toSyncResponse(sheetSyncService.syncGoogleSheetSource(sourceId), "singleSource");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminSyncResponse>> createSheetSync(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!adminAuthService.isAuthorized(authorization)) {
            return unauthorized();
        }

        return toSyncResponse(sheetSyncService.syncFromGoogleSheetUrl(), "allSources");
    }

    private ResponseEntity<ApiResponse<AdminSyncResponse>> toSyncResponse(SyncRunResult result, String source) {
        if (result.status() == SyncRunStatus.ALREADY_RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("SYNC_IN_PROGRESS", "Google Sheet 正在同步中，請稍後再試。"));
        }
        if (result.status() == SyncRunStatus.NO_SOURCES) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SYNC_SOURCE_NOT_CONFIGURED", "尚未設定 Google Sheet URL。"));
        }
        if (result.status() == SyncRunStatus.SOURCE_NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無指定表單連結。"));
        }

        String status = result.failedSources() > 0 ? "partial" : "synced";
        AdminSyncResponse response = new AdminSyncResponse(
                status,
                source,
                result.syncedAt(),
                result.totalSources(),
                result.syncedSources(),
                result.failedSources(),
                result.warnings().stream()
                        .map(warning -> new AdminSyncWarningDto(
                                warning.source(),
                                warning.sheetName(),
                                warning.rowNumber(),
                                warning.buyerNickname(),
                                warning.itemName(),
                                warning.message()))
                        .toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private SheetSyncSettingsResponse buildSettingsResponse() {
        List<SheetSyncSourceDto> sources = sheetSyncService.listSyncSources()
                .stream()
                .map(this::toSourceDto)
                .toList();
        return new SheetSyncSettingsResponse(sheetSyncService.isAutoSyncEnabled(), sources);
    }

    private SheetSyncSourceDto toSourceDto(SheetSyncSource source) {
        return new SheetSyncSourceDto(
                source.getId(),
                source.getDisplayName(),
                source.getSheetUrl(),
                source.getDefaultSourceType(),
                source.getLastSyncedAt());
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "登入已逾時，請重新登入。"));
    }
}

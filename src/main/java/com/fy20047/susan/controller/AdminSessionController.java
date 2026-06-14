package com.fy20047.susan.controller;

import com.fy20047.susan.dto.AdminLoginRequest;
import com.fy20047.susan.dto.AdminLoginResponse;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.service.AdminAuthService;
import com.fy20047.susan.service.AdminAuthService.AdminSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sessions")
public class AdminSessionController {

    private final AdminAuthService adminAuthService;

    public AdminSessionController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminLoginResponse>> createSession(
            @RequestBody(required = false) AdminLoginRequest request
    ) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "帳號或密碼錯誤。"));
        }
        AdminSession session = adminAuthService.createSession(request.username(), request.password());
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "帳號或密碼錯誤。"));
        }

        AdminLoginResponse response = new AdminLoginResponse(session.token(), session.expiresAt());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteCurrentSession(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        adminAuthService.revokeSession(authorization);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "ok")));
    }
}

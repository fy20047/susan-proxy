package com.fy20047.susan.dto;

import java.time.LocalDateTime;

public record AdminLoginResponse(String token, LocalDateTime expiresAt) {
}

package com.fy20047.susan.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private final String adminUsername;
    private final String adminPassword;
    private final Map<String, LocalDateTime> sessions = new ConcurrentHashMap<>();

    public AdminAuthService(
            @Value("${app.admin.username:catcanfly1215}") String adminUsername,
            @Value("${app.admin.password:Aa0425228305}") String adminPassword
    ) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public AdminSession createSession(String username, String password) {
        pruneExpiredSessions();
        if (!credentialsMatch(username, password)) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plus(SESSION_TTL);
        sessions.put(token, expiresAt);
        return new AdminSession(token, expiresAt);
    }

    public boolean isAuthorized(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return false;
        }

        LocalDateTime expiresAt = sessions.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            sessions.remove(token);
            return false;
        }
        return true;
    }

    public void revokeSession(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null) {
            sessions.remove(token);
        }
    }

    private boolean credentialsMatch(String username, String password) {
        return constantTimeEquals(adminUsername, normalize(username))
                && constantTimeEquals(adminPassword, normalize(password));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void pruneExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = normalize(expected).getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = normalize(actual).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record AdminSession(String token, LocalDateTime expiresAt) {
    }
}

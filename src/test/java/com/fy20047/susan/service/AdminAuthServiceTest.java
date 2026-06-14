package com.fy20047.susan.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fy20047.susan.service.AdminAuthService.AdminSession;
import org.junit.jupiter.api.Test;

class AdminAuthServiceTest {

    @Test
    void createsSessionForValidCredentials() {
        AdminAuthService service = new AdminAuthService("admin", "secret");

        AdminSession session = service.createSession("admin", "secret");

        assertNotNull(session);
        assertTrue(service.isAuthorized("Bearer " + session.token()));
    }

    @Test
    void rejectsInvalidCredentials() {
        AdminAuthService service = new AdminAuthService("admin", "secret");

        AdminSession session = service.createSession("admin", "wrong-password");

        assertNull(session);
    }

    @Test
    void revokesSession() {
        AdminAuthService service = new AdminAuthService("admin", "secret");
        AdminSession session = service.createSession("admin", "secret");
        String authorization = "Bearer " + session.token();

        service.revokeSession(authorization);

        assertFalse(service.isAuthorized(authorization));
    }
}

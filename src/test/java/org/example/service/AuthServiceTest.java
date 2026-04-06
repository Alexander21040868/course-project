package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    AuthService authService;

    private static String uniq(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void registerAndLogin() {
        String user = uniq("testuser");
        AuthResponse reg = authService.register(new AuthRequest(user, "pass123", user + "@t.com"));
        assertNotNull(reg.token());
        assertEquals(user, reg.username());
        assertEquals("STUDENT", reg.role());

        AuthResponse login = authService.login(new AuthRequest(user, "pass123", null));
        assertNotNull(login.token());
    }

    @Test
    void duplicateUsername() {
        String user = uniq("dup");
        authService.register(new AuthRequest(user, "pass", user + "@t.com"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(new AuthRequest(user, "pass", "other@t.com")));
    }

    @Test
    void wrongPassword() {
        String user = uniq("wp");
        authService.register(new AuthRequest(user, "correct", user + "@t.com"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new AuthRequest(user, "wrong", null)));
    }
}

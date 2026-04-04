package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Test
    void registerAndLogin() {
        AuthResponse reg = authService.register(new AuthRequest("testuser", "pass123", "test@t.com"));
        assertNotNull(reg.token());
        assertEquals("testuser", reg.username());
        assertEquals("STUDENT", reg.role());

        AuthResponse login = authService.login(new AuthRequest("testuser", "pass123", null));
        assertNotNull(login.token());
    }

    @Test
    void duplicateUsername() {
        authService.register(new AuthRequest("dup_user", "pass", "dup@t.com"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(new AuthRequest("dup_user", "pass", "dup2@t.com")));
    }

    @Test
    void wrongPassword() {
        authService.register(new AuthRequest("wp_user", "correct", "wp@t.com"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new AuthRequest("wp_user", "wrong", null)));
    }
}

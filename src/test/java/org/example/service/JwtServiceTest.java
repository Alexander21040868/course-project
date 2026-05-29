package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    JwtService jwtService;

    @Test
    void generateAndValidate() {
        String token = jwtService.generateToken("alice", "STUDENT");

        assertTrue(jwtService.isValid(token));
        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals("STUDENT", jwtService.extractRole(token));
    }

    @Test
    void invalidToken() {
        assertFalse(jwtService.isValid("garbage.token.here"));
    }
}

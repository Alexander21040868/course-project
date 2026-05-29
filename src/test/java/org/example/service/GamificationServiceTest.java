package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GamificationServiceTest {

    @Autowired
    GamificationService svc;

    @Test
    void levelFormula() {
        assertEquals(1, svc.calculateLevel(0));
        assertEquals(1, svc.calculateLevel(99));
        assertEquals(2, svc.calculateLevel(100));
        assertEquals(3, svc.calculateLevel(400));
        assertEquals(4, svc.calculateLevel(900));
    }

    @Test
    void xpToNextLevel() {
        assertEquals(100, svc.xpToNextLevel(0));
        assertEquals(1, svc.xpToNextLevel(99));
        assertEquals(300, svc.xpToNextLevel(100));
    }
}

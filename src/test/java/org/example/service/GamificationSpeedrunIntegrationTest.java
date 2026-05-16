package org.example.service;

import org.example.dto.AuthRequest;
import org.example.repository.UserRepository;
import org.example.support.TestId;
import org.example.testsupport.TestSandboxConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import({TestSandboxConfig.class, GamificationSpeedrunIntegrationTest.NightClock.class})
class GamificationSpeedrunIntegrationTest {

    @Autowired
    GamificationService gamificationService;
    @Autowired
    AuthService authService;
    @Autowired
    UserRepository userRepo;

    @TestConfiguration
    static class NightClock {
        @Bean
        @Primary
        Clock nightClock() {
            return Clock.fixed(Instant.parse("2026-06-01T02:15:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void speedrunAchievement_atNight() {
        String st = TestId.uniq("sr_clk");
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        var u = userRepo.findByUsername(st).orElseThrow();
        gamificationService.checkAndGrantAchievements(u);
        assertTrue(gamificationService.getUserAchievements(u.getId()).stream()
                .anyMatch(a -> "SPEEDRUN".equals(a.code())));
    }
}

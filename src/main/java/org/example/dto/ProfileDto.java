package org.example.dto;

import java.util.List;

public record ProfileDto(
        String username,
        String role,
        int xp,
        int level,
        int xpToNextLevel,
        long solvedCount,
        List<AchievementDto> achievements
) {}

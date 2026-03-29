package org.example.dto;

public record AchievementDto(
        String code,
        String name,
        String description,
        String icon,
        int xpReward
) {}

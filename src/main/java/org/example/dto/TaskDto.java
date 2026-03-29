package org.example.dto;

public record TaskDto(
        Long id,
        Long lessonId,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String hints,
        int orderIndex,
        boolean solved
) {}

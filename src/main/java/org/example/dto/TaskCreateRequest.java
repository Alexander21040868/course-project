package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskCreateRequest(
        @NotNull Long lessonId,
        @NotBlank String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String expectedOutput,
        String hints,
        int orderIndex
) {}

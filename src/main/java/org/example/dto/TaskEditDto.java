package org.example.dto;

import java.util.List;

public record TaskEditDto(
        Long id,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String expectedOutput,
        String hints,
        String authorUsername,
        Long linkedChallengeId,
        List<TestCaseDto> examples
) {}

package org.example.dto;

import java.util.List;

public record TaskDto(
        Long id,
        Long lessonId,
        String lessonTitle,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String hints,
        int orderIndex,
        boolean solved,
        List<TestCaseDto> sampleTests
) {}

package org.example.dto;

import java.util.List;

public record TaskDto(
        Long id,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String hints,
        String authorUsername,
        boolean solved,
        List<TestCaseDto> sampleTests
) {}

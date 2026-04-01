package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TaskCreateRequest(
        @NotNull Long lessonId,
        @NotBlank String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String expectedOutput,
        String hints,
        int orderIndex,
        List<TestCaseInput> testCases
) {
    public record TestCaseInput(String input, String expectedOutput, boolean sample) {}
}

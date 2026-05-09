package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record TaskCreateRequest(
        @NotBlank String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String expectedOutput,
        String hints,
        List<TestCaseInput> testCases
) {
    public record TestCaseInput(String input, String expectedOutput, boolean sample) {}
}

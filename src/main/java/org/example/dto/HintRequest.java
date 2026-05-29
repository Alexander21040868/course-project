package org.example.dto;

import jakarta.validation.constraints.NotNull;

public record HintRequest(
        @NotNull Long taskId,
        String code,
        String output
) {}

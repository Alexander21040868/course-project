package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record ChallengeCreateRequest(
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    int bonusXp,
    @NotNull List<Long> taskIds
) {}

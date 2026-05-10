package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChallengeDto(
    Long id, String title, String description,
    LocalDateTime startTime, LocalDateTime endTime,
    int bonusXp, String createdByName,
    int taskCount, boolean joined, boolean active, boolean upcoming,
    List<Long> taskIds
) {}

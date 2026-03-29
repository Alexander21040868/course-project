package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponse(
        Long id,
        String status,
        String output,
        int xpEarned,
        List<AchievementDto> newAchievements,
        LocalDateTime submittedAt
) {}

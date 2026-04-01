package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProfileDto(
        String username,
        String role,
        int xp,
        int level,
        int xpToNextLevel,
        long solvedCount,
        long totalTasks,
        int streak,
        int maxStreak,
        LocalDateTime createdAt,
        List<LessonProgressDto> lessonsProgress,
        List<SubmissionHistoryDto> recentSubmissions,
        List<AchievementDto> achievements
) {}

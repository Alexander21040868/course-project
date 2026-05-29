package org.example.dto;

import java.util.List;

public record StudentDetailDto(
    Long userId, String username, int xp, int level,
    long totalSolved, List<LessonProgressDto> lessons
) {}

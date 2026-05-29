package org.example.dto;

public record StudentProgressDto(
    Long userId, String username, int xp, int level,
    long totalSolved, long totalTasks, double solvedPercent,
    Long groupId, String groupName
) {}

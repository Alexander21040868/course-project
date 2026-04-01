package org.example.dto;

public record LeaderboardEntryDto(int rank, String username, int xp, int level, long solvedCount) {}

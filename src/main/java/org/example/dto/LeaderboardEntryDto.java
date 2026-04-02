package org.example.dto;

public record LeaderboardEntryDto(int rank, String username, int rating, int xp, int level, long solvedCount) {}

package org.example.dto;

public record TaskProgressDto(Long taskId, String title, String difficulty, boolean solved, long attempts) {}

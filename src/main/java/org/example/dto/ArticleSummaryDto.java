package org.example.dto;

public record ArticleSummaryDto(Long id, String title, String category, String authorUsername, int orderIndex) {}

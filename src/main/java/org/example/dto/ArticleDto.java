package org.example.dto;

public record ArticleDto(
        Long id, String title, String content, String category, int orderIndex, String authorUsername
) {}

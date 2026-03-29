package org.example.dto;

public record LessonDto(
        Long id,
        String title,
        String description,
        String content,
        int orderIndex,
        String authorName,
        int taskCount
) {}

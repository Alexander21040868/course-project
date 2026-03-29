package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record LessonCreateRequest(
        @NotBlank String title,
        String description,
        String content,
        int orderIndex
) {}

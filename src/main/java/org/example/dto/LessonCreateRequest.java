package org.example.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LessonCreateRequest(
        @NotBlank String title,
        String description,
        String content,
        int orderIndex,
        List<Long> taskIds
) {}

package org.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LessonCreateRequest(
        @NotBlank String title,
        String description,
        String content,
        @Min(0) int orderIndex,
        List<Long> taskIds
) {}

package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record ArticleCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        String category,
        Integer orderIndex
) {}

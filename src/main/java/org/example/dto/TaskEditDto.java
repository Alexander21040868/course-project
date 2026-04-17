package org.example.dto;

import java.util.List;

/** Полные данные задачи для формы редактирования (преподаватель). */
public record TaskEditDto(
        Long id,
        Long lessonId,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String templateCode,
        String expectedOutput,
        String hints,
        int orderIndex,
        List<TestCaseDto> examples
) {}

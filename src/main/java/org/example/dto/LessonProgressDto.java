package org.example.dto;

import java.util.List;

public record LessonProgressDto(Long lessonId, String title, int solved, int total, List<TaskProgressDto> tasks) {}

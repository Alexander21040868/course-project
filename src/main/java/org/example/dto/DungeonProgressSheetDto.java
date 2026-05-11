package org.example.dto;

import java.util.List;

public record DungeonProgressSheetDto(
        int orderIndex,
        String lessonTitle,
        List<DungeonTaskColumnDto> tasks,
        List<DungeonStudentRowDto> students,
        String emptyFilterHint
) {}

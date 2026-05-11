package org.example.dto;

import java.util.List;

public record DungeonStudentRowDto(long userId, String username, List<Boolean> solved) {}

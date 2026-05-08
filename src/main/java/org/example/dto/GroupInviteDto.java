package org.example.dto;

import java.time.LocalDateTime;

public record GroupInviteDto(
        Long id,
        String teacherUsername,
        String groupName,
        LocalDateTime createdAt
) {}

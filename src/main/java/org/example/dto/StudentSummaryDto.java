package org.example.dto;

public record StudentSummaryDto(
        Long userId,
        String username,
        String teacherUsername,
        boolean inviteSent
) {}

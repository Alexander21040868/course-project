package org.example.dto;

import java.time.LocalDateTime;

public record SubmissionHistoryDto(Long id, String taskTitle, String status, String output, LocalDateTime submittedAt) {}

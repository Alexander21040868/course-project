package org.example.dto;

import java.time.LocalDateTime;

public record NotificationDto(Long id, String title, String message, boolean read, LocalDateTime createdAt) {}

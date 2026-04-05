package org.example.controller;

import org.example.dto.NotificationDto;
import org.example.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> unread(Principal principal) {
        return ResponseEntity.ok(notificationService.unreadForUser(principal.getName()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(Principal principal) {
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount(principal.getName())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Principal principal) {
        notificationService.markRead(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}

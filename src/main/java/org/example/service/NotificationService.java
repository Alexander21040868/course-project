package org.example.service;

import org.example.dto.NotificationDto;
import org.example.entity.Notification;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void notifyUser(Long userId, String title, String message) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setReadFlag(false);
        notificationRepo.save(n);
    }

    @Transactional
    public void notifyAllStudents(String title, String message) {
        userRepo.findByRole(Role.STUDENT).forEach(u -> notifyUser(u.getId(), title, message));
    }

    public List<NotificationDto> unreadForUser(String username) {
        Long uid = userRepo.findByUsername(username).map(User::getId).orElseThrow(
                () -> new IllegalArgumentException("Пользователь не найден"));
        return notificationRepo.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(uid).stream()
                .map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.isReadFlag(), n.getCreatedAt()))
                .toList();
    }

    public long unreadCount(String username) {
        Long uid = userRepo.findByUsername(username).map(User::getId).orElse(0L);
        if (uid == 0) return 0;
        return notificationRepo.countByUserIdAndReadFlagFalse(uid);
    }

    @Transactional
    public void markRead(Long notificationId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Уведомление не найдено"));
        if (!n.getUser().getId().equals(user.getId()))
            throw new IllegalArgumentException("Нет доступа");
        n.setReadFlag(true);
    }
}

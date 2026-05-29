package org.example.repository;

import org.example.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFlagFalse(Long userId);
}

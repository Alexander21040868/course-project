package org.example.util;

import org.example.entity.Lesson;
import org.example.entity.Role;
import org.example.entity.Task;
import org.example.entity.User;

import java.time.LocalDateTime;

public final class TaskCatalog {

    private TaskCatalog() {}

    public static boolean isReleased(Task task, LocalDateTime now) {
        LocalDateTime v = task.getCatalogVisibleFrom();
        return v == null || !now.isBefore(v);
    }

    public static boolean canViewTask(Task task, User viewer, LocalDateTime now, Lesson lesson,
                                      Long challengeOrganizerUserId) {
        if (isReleased(task, now)) {
            return true;
        }
        if (challengeOrganizerUserId != null) {
            return challengeOrganizerUserId.equals(viewer.getId());
        }
        if (task.getAuthor() != null && task.getAuthor().getId().equals(viewer.getId())) {
            return true;
        }
        return lesson != null
                && viewer.getRole() == Role.TEACHER
                && lesson.getAuthor().getId().equals(viewer.getId());
    }
}

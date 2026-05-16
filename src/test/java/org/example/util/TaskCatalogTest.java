package org.example.util;

import org.example.entity.Role;
import org.example.entity.Task;
import org.example.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskCatalogTest {

    @Test
    void isReleased_nullVisibleFrom_isReleased() {
        Task t = new Task();
        t.setCatalogVisibleFrom(null);
        assertTrue(TaskCatalog.isReleased(t, LocalDateTime.now()));
    }

    @Test
    void isReleased_futureVisible_notReleasedYet() {
        Task t = new Task();
        t.setCatalogVisibleFrom(LocalDateTime.now().plusDays(1));
        assertFalse(TaskCatalog.isReleased(t, LocalDateTime.now()));
    }

    @Test
    void canViewTask_releasedVisibleToStudent() {
        Task t = new Task();
        t.setCatalogVisibleFrom(null);
        User student = new User();
        student.setRole(Role.STUDENT);
        assertTrue(TaskCatalog.canViewTask(t, student, LocalDateTime.now(), null, null));
    }

    @Test
    void canViewTask_unreleasedWithoutAuthor_studentCannotSee() {
        Task t = new Task();
        t.setCatalogVisibleFrom(LocalDateTime.now().plusDays(1));
        User student = new User();
        student.setRole(Role.STUDENT);
        assertFalse(TaskCatalog.canViewTask(t, student, LocalDateTime.now(), null, null));
    }
}

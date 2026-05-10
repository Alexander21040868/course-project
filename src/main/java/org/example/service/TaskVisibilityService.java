package org.example.service;

import org.example.entity.Lesson;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.repository.ChallengeRepository;
import org.example.util.TaskCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class TaskVisibilityService {

    private final ChallengeRepository challengeRepo;

    public TaskVisibilityService(ChallengeRepository challengeRepo) {
        this.challengeRepo = challengeRepo;
    }

    public boolean canView(Task task, User viewer, LocalDateTime now, Lesson lesson) {
        Long organizerId = challengeRepo.findOrganizerUserIdByTaskId(task.getId()).orElse(null);
        return TaskCatalog.canViewTask(task, viewer, now, lesson, organizerId);
    }
}

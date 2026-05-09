package org.example.controller;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.example.service.GamificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@Transactional(readOnly = true)
public class ProfileController {

    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;
    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final LessonTaskRepository lessonTaskRepo;
    private final GamificationService gamificationService;

    public ProfileController(UserRepository userRepo, SubmissionRepository submissionRepo,
                             LessonRepository lessonRepo, TaskRepository taskRepo,
                             LessonTaskRepository lessonTaskRepo,
                             GamificationService gamificationService) {
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(Principal principal) {
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        long solvedCount = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);
        long totalTasks = taskRepo.count();
        List<AchievementDto> achievements = gamificationService.getUserAchievements(user.getId());

        List<Lesson> visibleLessons;
        if (user.getRole() == Role.TEACHER) {
            visibleLessons = lessonRepo.findByAuthorIdOrderByOrderIndexAsc(user.getId());
        } else if (user.getTeacher() != null) {
            visibleLessons = lessonRepo.findByAuthorIdOrderByOrderIndexAsc(user.getTeacher().getId());
        } else {
            visibleLessons = List.of();
        }

        List<LessonProgressDto> lessonsProgress = visibleLessons.stream()
                .map(lesson -> {
                    List<Task> tasks = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId()).stream()
                            .map(LessonTask::getTask).toList();
                    List<TaskProgressDto> taskProgress = tasks.stream().map(t -> {
                        boolean solved = submissionRepo.existsByUserIdAndTaskIdAndStatus(
                                user.getId(), t.getId(), SubmissionStatus.CORRECT);
                        long attempts = submissionRepo.findByUserIdAndTaskIdOrderBySubmittedAtDesc(
                                user.getId(), t.getId()).size();
                        return new TaskProgressDto(t.getId(), t.getTitle(),
                                t.getDifficulty().name(), solved, attempts);
                    }).toList();
                    int solved = (int) taskProgress.stream().filter(TaskProgressDto::solved).count();
                    return new LessonProgressDto(lesson.getId(), lesson.getTitle(),
                            solved, tasks.size(), taskProgress);
                }).toList();

        List<SubmissionHistoryDto> recent = submissionRepo.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream().limit(10)
                .map(s -> new SubmissionHistoryDto(s.getId(), s.getTask().getTitle(),
                        s.getStatus().name(), s.getOutput(), s.getCode(), s.getSubmittedAt()))
                .toList();

        return ResponseEntity.ok(new ProfileDto(
                user.getUsername(), user.getRole().name(),
                user.getXp(), user.getLevel(),
                gamificationService.xpToNextLevel(user.getXp()),
                user.getRating(),
                solvedCount, totalTasks,
                user.getStreak(), user.getMaxStreak(),
                user.getCreatedAt(),
                lessonsProgress, recent, achievements
        ));
    }
}

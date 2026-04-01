package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.example.service.ChallengeService;
import org.example.service.LessonService;
import org.example.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminController {

    private final LessonService lessonService;
    private final TaskService taskService;
    private final ChallengeService challengeService;
    private final UserRepository userRepo;
    private final TaskRepository taskRepo;
    private final LessonRepository lessonRepo;
    private final SubmissionRepository submissionRepo;

    public AdminController(LessonService lessonService, TaskService taskService,
                           ChallengeService challengeService, UserRepository userRepo,
                           TaskRepository taskRepo, LessonRepository lessonRepo,
                           SubmissionRepository submissionRepo) {
        this.lessonService = lessonService;
        this.taskService = taskService;
        this.challengeService = challengeService;
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
    }

    @PostMapping("/lessons")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody LessonCreateRequest req,
                                                   Principal principal) {
        return ResponseEntity.ok(lessonService.create(req, principal.getName()));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskCreateRequest req) {
        return ResponseEntity.ok(taskService.create(req));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/challenges")
    public ResponseEntity<ChallengeDto> createChallenge(@Valid @RequestBody ChallengeCreateRequest req,
                                                         Principal principal) {
        return ResponseEntity.ok(challengeService.create(req, principal.getName()));
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentProgressDto>> getStudentsProgress() {
        long totalTasks = taskRepo.count();
        List<StudentProgressDto> students = userRepo.findByRole(Role.STUDENT).stream()
                .map(u -> {
                    long solved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                            u.getId(), SubmissionStatus.CORRECT);
                    double pct = totalTasks > 0 ? (solved * 100.0 / totalTasks) : 0;
                    return new StudentProgressDto(u.getId(), u.getUsername(),
                            u.getXp(), u.getLevel(), solved, totalTasks, Math.round(pct * 10) / 10.0);
                })
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        long totalSolved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);

        List<LessonProgressDto> lessons = lessonRepo.findAllByOrderByOrderIndexAsc().stream()
                .map(lesson -> {
                    List<Task> tasks = taskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId());
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

        return ResponseEntity.ok(new StudentDetailDto(
                user.getId(), user.getUsername(), user.getXp(), user.getLevel(),
                totalSolved, lessons));
    }
}

package org.example.controller;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.example.service.GamificationService;
import org.example.service.LessonService;
import org.example.service.TaskVisibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final StudentTeacherRepository studentTeacherRepo;
    private final GamificationService gamificationService;
    private final LessonService lessonService;
    private final TaskVisibilityService taskVisibility;

    public ProfileController(UserRepository userRepo, SubmissionRepository submissionRepo,
                             LessonRepository lessonRepo, TaskRepository taskRepo,
                             LessonTaskRepository lessonTaskRepo,
                             StudentTeacherRepository studentTeacherRepo,
                             GamificationService gamificationService,
                             LessonService lessonService,
                             TaskVisibilityService taskVisibility) {
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.studentTeacherRepo = studentTeacherRepo;
        this.gamificationService = gamificationService;
        this.lessonService = lessonService;
        this.taskVisibility = taskVisibility;
    }

    @GetMapping("/my-teachers")
    public ResponseEntity<List<TeacherBriefDto>> myTeachers(Principal principal) {
        return ResponseEntity.ok(lessonService.listLinkedTeachersForStudent(principal.getName()));
    }

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(Principal principal) {
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        long solvedCount = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);
        long totalTasks = taskRepo.countReleasedAt(LocalDateTime.now());
        List<AchievementDto> achievements = gamificationService.getUserAchievements(user.getId());

        List<LessonProgressDto> lessonsProgress = buildLessonsProgress(user);

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

    private List<LessonProgressDto> buildLessonsProgress(User user) {
        List<LessonProgressDto> out = new ArrayList<>();
        if (user.getRole() == Role.TEACHER) {
            for (Lesson lesson : lessonRepo.findByAuthorIdOrderByOrderIndexAsc(user.getId())) {
                out.add(lessonProgress(user, lesson, null, lesson.getOrderIndex()));
            }
            return out;
        }
        List<User> teachers = studentTeacherRepo.findTeachersByStudentId(user.getId());
        if (teachers.isEmpty() && user.getTeacher() != null) {
            teachers = List.of(user.getTeacher());
        }
        for (User t : teachers) {
            for (Lesson lesson : lessonRepo.findByAuthorIdOrderByOrderIndexAsc(t.getId())) {
                out.add(lessonProgress(user, lesson, t.getUsername(), lesson.getOrderIndex()));
            }
        }
        return out;
    }

    private LessonProgressDto lessonProgress(User user, Lesson lesson, String teacherUsername, int dungeonOrder) {
        LocalDateTime now = LocalDateTime.now();
        List<Task> tasks = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId()).stream()
                .map(LessonTask::getTask)
                .filter(t -> taskVisibility.canView(t, user, now, lesson))
                .toList();
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
                solved, tasks.size(), taskProgress, teacherUsername, dungeonOrder);
    }
}

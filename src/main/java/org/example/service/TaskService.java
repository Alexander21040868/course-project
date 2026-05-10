package org.example.service;

import org.example.dto.TaskCreateRequest;
import org.example.dto.TaskDto;
import org.example.dto.TaskEditDto;
import org.example.dto.TestCaseDto;
import org.example.entity.*;
import org.example.repository.*;
import org.example.util.XpLimits;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepo;
    private final SubmissionRepository submissionRepo;
    private final UserRepository userRepo;
    private final TestCaseRepository testCaseRepo;
    private final LessonTaskRepository lessonTaskRepo;
    private final LessonRepository lessonRepo;
    private final StudentTeacherRepository studentTeacherRepo;
    private final ChallengeRepository challengeRepo;
    private final ChallengeService challengeService;
    private final TaskVisibilityService taskVisibility;

    public TaskService(TaskRepository taskRepo, SubmissionRepository submissionRepo,
                       UserRepository userRepo, TestCaseRepository testCaseRepo,
                       LessonTaskRepository lessonTaskRepo, LessonRepository lessonRepo,
                       StudentTeacherRepository studentTeacherRepo,
                       ChallengeRepository challengeRepo,
                       ChallengeService challengeService,
                       TaskVisibilityService taskVisibility) {
        this.taskRepo = taskRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
        this.testCaseRepo = testCaseRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.lessonRepo = lessonRepo;
        this.studentTeacherRepo = studentTeacherRepo;
        this.challengeRepo = challengeRepo;
        this.challengeService = challengeService;
        this.taskVisibility = taskVisibility;
    }

    public Page<TaskDto> findPage(String search, String username, int page, int size) {
        User viewer = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        long vid = viewer.getId();
        LocalDateTime now = LocalDateTime.now();
        var p = PageRequest.of(page, size);
        if (search != null && !search.isBlank()) {
            String t = search.trim();
            Optional<Long> byId = parseNumericIdQuery(t);
            if (byId.isPresent()) {
                if (page > 0) {
                    return Page.<TaskDto>empty(p);
                }
                Optional<Task> hit = taskRepo.findById(byId.get())
                        .filter(task -> taskVisibility.canView(task, viewer, now, null));
                if (hit.isPresent()) {
                    return new PageImpl<>(List.of(toDto(hit.get(), vid)), p, 1);
                }
                return Page.<TaskDto>empty(p);
            }
            return taskRepo.searchCatalog(t, now, vid, p).map(task -> toDto(task, vid));
        }
        return taskRepo.pageCatalog(now, vid, p).map(task -> toDto(task, vid));
    }

    private static Optional<Long> parseNumericIdQuery(String trimmed) {
        if (!trimmed.matches("#?\\d+")) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(trimmed.replaceFirst("^#", "")));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public List<TaskDto> findByLesson(Long lessonId, String username) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
        User viewer = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (viewer.getRole() == Role.TEACHER) {
            if (!lesson.getAuthor().getId().equals(viewer.getId())) {
                throw new IllegalArgumentException("Этот урок создан другим учителем");
            }
        } else {
            Long authorId = lesson.getAuthor().getId();
            if (!studentTeacherRepo.existsByStudentIdAndTeacherId(viewer.getId(), authorId)) {
                User teacher = viewer.getTeacher();
                if (teacher == null || !authorId.equals(teacher.getId())) {
                    throw new IllegalArgumentException("Урок недоступен: вы не прикреплены к его автору");
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        return lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lessonId).stream()
                .map(LessonTask::getTask)
                .filter(task -> taskVisibility.canView(task, viewer, now, lesson))
                .map(task -> toDto(task, viewer.getId()))
                .toList();
    }

    public TaskDto findById(Long id, String username) {
        User viewer = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Task task = loadVisibleTask(id, viewer);
        return toDto(task, viewer.getId());
    }

    public Task requireTaskForLearner(Long id, String username) {
        User viewer = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return loadVisibleTask(id, viewer);
    }

    public void assertCanSubmit(Task task, User user) {
        if (!taskVisibility.canView(task, user, LocalDateTime.now(), null)) {
            throw new IllegalArgumentException("Задача недоступна");
        }
    }

    private Task loadVisibleTask(Long id, User viewer) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        if (!taskVisibility.canView(task, viewer, LocalDateTime.now(), null)) {
            throw new IllegalArgumentException("Задача не найдена");
        }
        return task;
    }

    public TaskEditDto findForEdit(Long id) {
        Task t = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        List<TestCaseDto> examples = testCaseRepo.findByTaskIdOrderByOrderIndexAsc(id).stream()
                .map(tc -> new TestCaseDto(tc.getId(), tc.getInput(), tc.getExpectedOutput(), tc.isSample()))
                .toList();
        return new TaskEditDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDifficulty().name(),
                t.getXpReward(),
                t.getTemplateCode(),
                t.getExpectedOutput(),
                t.getHints(),
                t.getAuthor() != null ? t.getAuthor().getUsername() : null,
                challengeService.linkedChallengeId(id),
                examples
        );
    }

    @Transactional(readOnly = false)
    public TaskDto create(TaskCreateRequest req, String authorUsername) {
        User author = userRepo.findByUsername(authorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Task task = new Task();
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setDifficulty(req.difficulty() != null ? Difficulty.valueOf(req.difficulty()) : Difficulty.EASY);
        task.setXpReward(XpLimits.normalizeTaskXp(req.xpReward()));
        task.setTemplateCode(req.templateCode());
        task.setExpectedOutput(req.expectedOutput());
        task.setHints(req.hints());
        task.setAuthor(author);
        if (req.challengeId() != null) {
            Challenge ch = challengeRepo.findById(req.challengeId())
                    .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));
            task.setCatalogVisibleFrom(ch.getStartTime());
        }
        taskRepo.save(task);

        saveTestCases(task, req.testCases());
        if (req.challengeId() != null) {
            challengeService.attachTask(req.challengeId(), task.getId(), authorUsername);
        }

        return toDto(task, null);
    }

    @Transactional(readOnly = false)
    public TaskDto update(Long id, TaskCreateRequest req) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        task.setTitle(req.title());
        task.setDescription(req.description());
        if (req.difficulty() != null) task.setDifficulty(Difficulty.valueOf(req.difficulty()));
        task.setXpReward(XpLimits.normalizeTaskXp(req.xpReward()));
        task.setTemplateCode(req.templateCode());
        task.setExpectedOutput(req.expectedOutput());
        task.setHints(req.hints());
        task = taskRepo.save(task);

        testCaseRepo.deleteByTaskId(id);
        saveTestCases(task, req.testCases());

        return toDto(task, null);
    }

    @Transactional(readOnly = false)
    public void delete(Long id) {
        lessonTaskRepo.deleteByTaskId(id);
        taskRepo.deleteById(id);
    }

    private void saveTestCases(Task task, List<TaskCreateRequest.TestCaseInput> cases) {
        if (cases == null || cases.isEmpty()) return;
        int idx = 0;
        for (TaskCreateRequest.TestCaseInput tci : cases) {
            if (tci.expectedOutput() == null || tci.expectedOutput().isBlank()) continue;
            TestCase tc = new TestCase();
            tc.setTask(task);
            tc.setInput(tci.input() == null ? "" : tci.input());
            tc.setExpectedOutput(tci.expectedOutput());
            tc.setSample(tci.sample());
            tc.setOrderIndex(idx++);
            testCaseRepo.save(tc);
        }
    }

    private TaskDto toDto(Task t, Long userId) {
        boolean solved = userId != null &&
                submissionRepo.existsByUserIdAndTaskIdAndStatus(userId, t.getId(), SubmissionStatus.CORRECT);

        List<TestCaseDto> sampleTests = testCaseRepo.findByTaskIdAndSampleTrueOrderByOrderIndexAsc(t.getId())
                .stream()
                .map(tc -> new TestCaseDto(tc.getId(), tc.getInput(), tc.getExpectedOutput(), true))
                .toList();

        return new TaskDto(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getDifficulty().name(), t.getXpReward(), t.getTemplateCode(),
                t.getHints(),
                t.getAuthor() != null ? t.getAuthor().getUsername() : null,
                solved, sampleTests
        );
    }
}

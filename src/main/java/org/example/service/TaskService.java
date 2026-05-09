package org.example.service;

import org.example.dto.TaskCreateRequest;
import org.example.dto.TaskDto;
import org.example.dto.TaskEditDto;
import org.example.dto.TestCaseDto;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TaskService(TaskRepository taskRepo, SubmissionRepository submissionRepo,
                       UserRepository userRepo, TestCaseRepository testCaseRepo,
                       LessonTaskRepository lessonTaskRepo, LessonRepository lessonRepo) {
        this.taskRepo = taskRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
        this.testCaseRepo = testCaseRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.lessonRepo = lessonRepo;
    }

    public Page<TaskDto> findPage(String search, String username, int page, int size) {
        Long userId = resolveUserId(username);
        var p = PageRequest.of(page, size);
        if (search != null && !search.isBlank()) {
            String t = search.trim();
            Optional<Long> byId = parseNumericIdQuery(t);
            if (byId.isPresent()) {
                if (page > 0) return Page.<TaskDto>empty(p);
                return taskRepo.findById(byId.get())
                        .map(task -> (Page<TaskDto>) new PageImpl<>(List.of(toDto(task, userId)), p, 1))
                        .orElseGet(() -> Page.<TaskDto>empty(p));
            }
            return taskRepo.findByTitleContainingIgnoreCaseOrderByIdAsc(t, p)
                    .map(task -> toDto(task, userId));
        }
        return taskRepo.findAllByOrderByIdAsc(p).map(task -> toDto(task, userId));
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
            User teacher = viewer.getTeacher();
            if (teacher == null || !lesson.getAuthor().getId().equals(teacher.getId())) {
                throw new IllegalArgumentException("Урок недоступен: вы не прикреплены к его автору");
            }
        }
        return lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lessonId).stream()
                .map(lt -> toDto(lt.getTask(), viewer.getId()))
                .toList();
    }

    public TaskDto findById(Long id, String username) {
        Long userId = resolveUserId(username);
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        return toDto(task, userId);
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
        task.setXpReward(req.xpReward() > 0 ? req.xpReward() : 10);
        task.setTemplateCode(req.templateCode());
        task.setExpectedOutput(req.expectedOutput());
        task.setHints(req.hints());
        task.setAuthor(author);
        taskRepo.save(task);

        saveTestCases(task, req.testCases());

        return toDto(task, null);
    }

    @Transactional(readOnly = false)
    public TaskDto update(Long id, TaskCreateRequest req) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        task.setTitle(req.title());
        task.setDescription(req.description());
        if (req.difficulty() != null) task.setDifficulty(Difficulty.valueOf(req.difficulty()));
        if (req.xpReward() > 0) task.setXpReward(req.xpReward());
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

    private Long resolveUserId(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();
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

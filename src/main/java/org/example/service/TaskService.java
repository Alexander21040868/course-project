package org.example.service;

import org.example.dto.TaskCreateRequest;
import org.example.dto.TaskDto;
import org.example.dto.TestCaseDto;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepo;
    private final LessonRepository lessonRepo;
    private final SubmissionRepository submissionRepo;
    private final UserRepository userRepo;
    private final TestCaseRepository testCaseRepo;

    public TaskService(TaskRepository taskRepo, LessonRepository lessonRepo,
                       SubmissionRepository submissionRepo, UserRepository userRepo,
                       TestCaseRepository testCaseRepo) {
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
        this.testCaseRepo = testCaseRepo;
    }

    public List<TaskDto> findAll(String search, String username) {
        Long userId = resolveUserId(username);
        List<Task> tasks = (search != null && !search.isBlank())
                ? taskRepo.findByTitleContainingIgnoreCaseOrderByIdAsc(search)
                : taskRepo.findAllByOrderByIdAsc();
        return tasks.stream().map(t -> toDto(t, userId)).toList();
    }

    public List<TaskDto> findByLesson(Long lessonId, String username) {
        Long userId = resolveUserId(username);
        return taskRepo.findByLessonIdOrderByOrderIndexAsc(lessonId).stream()
                .map(t -> toDto(t, userId)).toList();
    }

    public TaskDto findById(Long id, String username) {
        Long userId = resolveUserId(username);
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        return toDto(task, userId);
    }

    @Transactional
    public TaskDto create(TaskCreateRequest req) {
        Lesson lesson = lessonRepo.findById(req.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));

        Task task = new Task();
        task.setLesson(lesson);
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setDifficulty(req.difficulty() != null ? Difficulty.valueOf(req.difficulty()) : Difficulty.EASY);
        task.setXpReward(req.xpReward() > 0 ? req.xpReward() : 10);
        task.setTemplateCode(req.templateCode());
        task.setExpectedOutput(req.expectedOutput());
        task.setHints(req.hints());
        task.setOrderIndex(req.orderIndex());
        taskRepo.save(task);

        if (req.testCases() != null) {
            int idx = 0;
            for (TaskCreateRequest.TestCaseInput tci : req.testCases()) {
                TestCase tc = new TestCase();
                tc.setTask(task);
                tc.setInput(tci.input());
                tc.setExpectedOutput(tci.expectedOutput());
                tc.setSample(tci.sample());
                tc.setOrderIndex(idx++);
                testCaseRepo.save(tc);
            }
        }

        return toDto(task, null);
    }

    @Transactional
    public void delete(Long id) {
        taskRepo.deleteById(id);
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
                t.getId(), t.getLesson().getId(), t.getLesson().getTitle(),
                t.getTitle(), t.getDescription(),
                t.getDifficulty().name(), t.getXpReward(), t.getTemplateCode(),
                t.getHints(), t.getOrderIndex(), solved, sampleTests
        );
    }
}

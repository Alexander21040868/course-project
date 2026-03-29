package org.example.service;

import org.example.dto.TaskCreateRequest;
import org.example.dto.TaskDto;
import org.example.entity.Difficulty;
import org.example.entity.Lesson;
import org.example.entity.SubmissionStatus;
import org.example.entity.Task;
import org.example.repository.LessonRepository;
import org.example.repository.SubmissionRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
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

    public TaskService(TaskRepository taskRepo, LessonRepository lessonRepo,
                       SubmissionRepository submissionRepo, UserRepository userRepo) {
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
    }

    public List<TaskDto> findByLesson(Long lessonId, String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();

        return taskRepo.findByLessonIdOrderByOrderIndexAsc(lessonId).stream()
                .map(t -> toDto(t, userId))
                .toList();
    }

    public TaskDto findById(Long id, String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();

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

        return toDto(taskRepo.save(task), null);
    }

    @Transactional
    public void delete(Long id) {
        taskRepo.deleteById(id);
    }

    private TaskDto toDto(Task t, Long userId) {
        boolean solved = userId != null &&
                submissionRepo.existsByUserIdAndTaskIdAndStatus(userId, t.getId(), SubmissionStatus.CORRECT);
        return new TaskDto(
                t.getId(), t.getLesson().getId(), t.getTitle(), t.getDescription(),
                t.getDifficulty().name(), t.getXpReward(), t.getTemplateCode(),
                t.getHints(), t.getOrderIndex(), solved
        );
    }
}

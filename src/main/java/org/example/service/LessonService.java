package org.example.service;

import org.example.dto.LessonCreateRequest;
import org.example.dto.LessonDto;
import org.example.entity.Lesson;
import org.example.entity.LessonTask;
import org.example.entity.Role;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.repository.LessonRepository;
import org.example.repository.LessonTaskRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final LessonTaskRepository lessonTaskRepo;
    private final NotificationService notificationService;

    public LessonService(LessonRepository lessonRepo, TaskRepository taskRepo, UserRepository userRepo,
                         LessonTaskRepository lessonTaskRepo, NotificationService notificationService) {
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.notificationService = notificationService;
    }

    public Page<LessonDto> findPageFor(String username, int page, int size) {
        User viewer = userRepo.findByUsername(username).orElse(null);
        var pageable = PageRequest.of(page, size);
        if (viewer == null) return Page.empty(pageable);
        if (viewer.getRole() == Role.TEACHER) {
            return lessonRepo.findByAuthorIdOrderByOrderIndexAsc(viewer.getId(), pageable)
                    .map(this::toDto);
        }
        User teacher = viewer.getTeacher();
        if (teacher == null) return new PageImpl<>(List.of(), pageable, 0);
        return lessonRepo.findByAuthorIdOrderByOrderIndexAsc(teacher.getId(), pageable)
                .map(this::toDto);
    }

    public LessonDto findById(Long id, String username) {
        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
        ensureCanView(lesson, username);
        return toDto(lesson);
    }

    public LessonDto findByOrderForTeacher(int orderIndex, String teacherUsername) {
        User teacher = userRepo.findByUsername(teacherUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Доступно только преподавателям");
        }
        Lesson lesson = lessonRepo.findByAuthorIdAndOrderIndex(teacher.getId(), orderIndex)
                .orElseThrow(() -> new IllegalArgumentException("Подземелье №" + orderIndex + " у вас не найдено"));
        return toDto(lesson);
    }

    public void ensureCanView(Lesson lesson, String username) {
        User viewer = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (viewer.getRole() == Role.TEACHER) {
            if (!lesson.getAuthor().getId().equals(viewer.getId())) {
                throw new IllegalArgumentException("Этот урок создан другим учителем");
            }
            return;
        }
        User teacher = viewer.getTeacher();
        if (teacher == null || !lesson.getAuthor().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Урок недоступен: вы не прикреплены к его автору");
        }
    }

    @Transactional(readOnly = false)
    public LessonDto create(LessonCreateRequest req, String authorUsername) {
        User author = userRepo.findByUsername(authorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Lesson lesson = new Lesson();
        lesson.setTitle(req.title());
        lesson.setDescription(req.description());
        lesson.setContent(req.content());
        lesson.setOrderIndex(req.orderIndex());
        lesson.setAuthor(author);

        Lesson saved = lessonRepo.save(lesson);
        if (req.taskIds() != null && !req.taskIds().isEmpty()) {
            attachTasks(saved.getId(), req.taskIds());
        }
        log.info("Урок создан: id={} title={}", saved.getId(), saved.getTitle());
        notificationService.notifyAllStudents("Новый урок", "Опубликовано подземелье: " + saved.getTitle());
        return toDto(saved);
    }

    @Transactional(readOnly = false)
    public LessonDto update(Long id, LessonCreateRequest req) {
        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
        lesson.setTitle(req.title());
        lesson.setDescription(req.description());
        lesson.setContent(req.content());
        lesson.setOrderIndex(req.orderIndex());
        Lesson saved = lessonRepo.save(lesson);
        if (req.taskIds() != null && !req.taskIds().isEmpty()) {
            attachTasks(saved.getId(), req.taskIds());
        }
        log.info("Урок обновлён: id={}", id);
        return toDto(saved);
    }

    @Transactional(readOnly = false)
    public void delete(Long id) {
        log.info("Урок удалён: id={}", id);
        lessonTaskRepo.deleteByLessonId(id);
        lessonRepo.deleteById(id);
    }

    @Transactional(readOnly = false)
    public List<Long> attachTasks(Long lessonId, List<Long> taskIds) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));

        List<LessonTask> existing = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lessonId);
        int nextIndex = existing.stream().mapToInt(LessonTask::getOrderIndex).max().orElse(0) + 1;

        List<Long> attached = new ArrayList<>();
        for (Long taskId : new LinkedHashSet<>(taskIds)) {
            Task task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Задача #" + taskId + " не найдена"));
            if (lessonTaskRepo.findByLessonIdAndTaskId(lessonId, taskId).isEmpty()) {
                lessonTaskRepo.save(new LessonTask(lesson, task, nextIndex++));
            }
            attached.add(task.getId());
        }
        return attached;
    }

    @Transactional(readOnly = false)
    public void detachTask(Long lessonId, Long taskId, String teacherUsername) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
        ensureCanView(lesson, teacherUsername);
        lessonTaskRepo.deleteLink(lessonId, taskId);
    }

    private LessonDto toDto(Lesson l) {
        long taskCount = lessonTaskRepo.countByLessonId(l.getId());
        return new LessonDto(
                l.getId(), l.getTitle(), l.getDescription(), l.getContent(),
                l.getOrderIndex(), l.getAuthor().getUsername(), (int) taskCount
        );
    }
}

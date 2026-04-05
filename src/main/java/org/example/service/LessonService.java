package org.example.service;

import org.example.dto.LessonCreateRequest;
import org.example.dto.LessonDto;
import org.example.entity.Lesson;
import org.example.entity.User;
import org.example.repository.LessonRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    public LessonService(LessonRepository lessonRepo, TaskRepository taskRepo, UserRepository userRepo,
                         NotificationService notificationService) {
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
    }

    public List<LessonDto> findAll() {
        return lessonRepo.findAllByOrderByOrderIndexAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public LessonDto findById(Long id) {
        return lessonRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
    }

    @Transactional
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
        notificationService.notifyAllStudents("Новый урок", "Опубликовано подземелье: " + saved.getTitle());
        return toDto(saved);
    }

    @Transactional
    public LessonDto update(Long id, LessonCreateRequest req) {
        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));
        lesson.setTitle(req.title());
        lesson.setDescription(req.description());
        lesson.setContent(req.content());
        lesson.setOrderIndex(req.orderIndex());
        return toDto(lessonRepo.save(lesson));
    }

    @Transactional
    public void delete(Long id) {
        lessonRepo.deleteById(id);
    }

    private LessonDto toDto(Lesson l) {
        int taskCount = taskRepo.findByLessonIdOrderByOrderIndexAsc(l.getId()).size();
        return new LessonDto(
                l.getId(), l.getTitle(), l.getDescription(), l.getContent(),
                l.getOrderIndex(), l.getAuthor().getUsername(), taskCount
        );
    }
}

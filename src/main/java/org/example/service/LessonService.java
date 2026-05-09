package org.example.service;

import org.example.dto.LessonCreateRequest;
import org.example.dto.LessonDto;
import org.example.dto.TeacherBriefDto;
import org.example.entity.Lesson;
import org.example.entity.LessonTask;
import org.example.entity.Role;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.repository.LessonRepository;
import org.example.repository.LessonTaskRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.example.repository.StudentTeacherRepository;
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
    private final StudentTeacherRepository studentTeacherRepo;
    private final NotificationService notificationService;

    public LessonService(LessonRepository lessonRepo, TaskRepository taskRepo, UserRepository userRepo,
                         LessonTaskRepository lessonTaskRepo, StudentTeacherRepository studentTeacherRepo,
                         NotificationService notificationService) {
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.studentTeacherRepo = studentTeacherRepo;
        this.notificationService = notificationService;
    }

    public Page<LessonDto> findPageFor(String username, String teacherUsernameParam, int page, int size) {
        User viewer = userRepo.findByUsername(username).orElse(null);
        var pageable = PageRequest.of(page, size);
        if (viewer == null) return Page.empty(pageable);
        if (viewer.getRole() == Role.TEACHER) {
            return lessonRepo.findByAuthorIdOrderByOrderIndexAsc(viewer.getId(), pageable)
                    .map(this::toDto);
        }
        User target = resolveQuestTeacherForStudent(viewer, teacherUsernameParam);
        if (target == null) return new PageImpl<>(List.of(), pageable, 0);
        return lessonRepo.findByAuthorIdOrderByOrderIndexAsc(target.getId(), pageable)
                .map(this::toDto);
    }

    private User resolveQuestTeacherForStudent(User student, String teacherUsernameParam) {
        List<User> linked = studentTeacherRepo.findTeachersByStudentId(student.getId());
        if (linked.isEmpty() && student.getTeacher() != null) {
            linked = List.of(student.getTeacher());
        }
        if (linked.isEmpty()) return null;

        if (teacherUsernameParam != null && !teacherUsernameParam.isBlank()) {
            String want = teacherUsernameParam.trim();
            return linked.stream()
                    .filter(t -> t.getUsername().equalsIgnoreCase(want))
                    .findFirst()
                    .orElse(null);
        }
        if (linked.size() == 1) return linked.get(0);
        return null;
    }

    public List<TeacherBriefDto> listLinkedTeachersForStudent(String studentUsername) {
        User student = userRepo.findByUsername(studentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (student.getRole() != Role.STUDENT) return List.of();
        List<User> linked = studentTeacherRepo.findTeachersByStudentId(student.getId());
        if (linked.isEmpty() && student.getTeacher() != null) {
            linked = List.of(student.getTeacher());
        }
        return linked.stream().map(t -> new TeacherBriefDto(t.getUsername())).toList();
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
                .orElseThrow(() -> new IllegalArgumentException("Подземелье #" + orderIndex + " у вас не найдено"));
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
        if (studentTeacherRepo.existsByStudentIdAndTeacherId(viewer.getId(), lesson.getAuthor().getId())) {
            return;
        }
        if (teacher != null && lesson.getAuthor().getId().equals(teacher.getId())) {
            return;
        }
        throw new IllegalArgumentException("Урок недоступен: вы не прикреплены к его автору");
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

        if (lessonRepo.existsByAuthorIdAndOrderIndex(author.getId(), req.orderIndex())) {
            throw new IllegalArgumentException("У вас уже есть подземелье с номером " + req.orderIndex());
        }

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
        if (req.orderIndex() != lesson.getOrderIndex()
                && lessonRepo.existsByAuthorIdAndOrderIndexAndIdNot(
                lesson.getAuthor().getId(), req.orderIndex(), lesson.getId())) {
            throw new IllegalArgumentException("У вас уже есть подземелье с номером " + req.orderIndex());
        }
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
    public LessonDto updateByOrderForTeacher(int currentOrderIndex, LessonCreateRequest req, String teacherUsername) {
        User teacher = userRepo.findByUsername(teacherUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Доступно только преподавателям");
        }
        Lesson lesson = lessonRepo.findByAuthorIdAndOrderIndex(teacher.getId(), currentOrderIndex)
                .orElseThrow(() -> new IllegalArgumentException("Подземелье #" + currentOrderIndex + " у вас не найдено"));
        if (req.orderIndex() != lesson.getOrderIndex()
                && lessonRepo.existsByAuthorIdAndOrderIndexAndIdNot(
                teacher.getId(), req.orderIndex(), lesson.getId())) {
            throw new IllegalArgumentException("У вас уже есть подземелье с номером " + req.orderIndex());
        }
        lesson.setTitle(req.title());
        lesson.setDescription(req.description());
        lesson.setContent(req.content());
        lesson.setOrderIndex(req.orderIndex());
        Lesson saved = lessonRepo.save(lesson);
        if (req.taskIds() != null && !req.taskIds().isEmpty()) {
            attachTasks(saved.getId(), req.taskIds());
        }
        log.info("Урок обновлён по порядку: было #{}, id={}", currentOrderIndex, lesson.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = false)
    public void deleteByOrderForTeacher(int orderIndex, String teacherUsername) {
        User teacher = userRepo.findByUsername(teacherUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Доступно только преподавателям");
        }
        Lesson lesson = lessonRepo.findByAuthorIdAndOrderIndex(teacher.getId(), orderIndex)
                .orElseThrow(() -> new IllegalArgumentException("Подземелье #" + orderIndex + " у вас не найдено"));
        delete(lesson.getId());
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

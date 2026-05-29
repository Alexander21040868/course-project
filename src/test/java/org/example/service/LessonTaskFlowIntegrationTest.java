package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.LessonCreateRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.UserRepository;
import org.example.support.TestId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LessonTaskFlowIntegrationTest {

    @Autowired
    AuthService authService;
    @Autowired
    TaskService taskService;
    @Autowired
    LessonService lessonService;
    @Autowired
    UserRepository userRepo;
    @Autowired
    StudentTeacherRepository studentTeacherRepo;

    @Test
    void teacherCreatesLessonWithTasks_studentSeesThemInLesson() {
        String teacher = TestId.uniq("teach");
        String student = TestId.uniq("stud");
        authService.register(new AuthRequest(teacher, "pw", teacher + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "pw", student + "@x.dev", "STUDENT"));

        long tid = userRepo.findByUsername(teacher).orElseThrow().getId();
        long sid = userRepo.findByUsername(student).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        var taskReq = new TaskCreateRequest(
                "T1", null, "EASY", 10,
                null, "out", null, null, null);
        long taskId = taskService.create(taskReq, teacher).id();

        int order = 71 + Math.floorMod(teacher.hashCode(), 500);
        var lessonReq = new LessonCreateRequest(
                "Dungeon A", "desc", "content", order,
                List.of(taskId));
        var lesson = lessonService.create(lessonReq, teacher);

        var tasks = taskService.findByLesson(lesson.id(), student);
        assertEquals(1, tasks.size());
        assertEquals(taskId, tasks.get(0).id());

        var page = lessonService.findPageFor(student, teacher, 0, 20);
        assertFalse(page.getContent().isEmpty());
        assertTrue(page.getContent().stream().anyMatch(l -> l.id().equals(lesson.id())));
    }

    @Test
    void attachTasksAfterLessonCreated() {
        String teacher = TestId.uniq("te2");
        authService.register(new AuthRequest(teacher, "pw", teacher + "@x.dev", "TEACHER"));

        int order = 80 + Math.floorMod(teacher.hashCode(), 400);
        var lesson = lessonService.create(
                new LessonCreateRequest("L", "", "", order, List.of()),
                teacher);
        long t1 = taskService.create(new TaskCreateRequest("a", null, "EASY", 1, null, "x", null, null, null), teacher).id();
        lessonService.attachTasks(lesson.id(), List.of(t1));

        var titles = taskService.findByLesson(lesson.id(), teacher).stream().map(t -> t.title()).toList();
        assertTrue(titles.contains("a"));
    }
}

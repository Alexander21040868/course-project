package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.*;
import org.example.service.LessonService;
import org.example.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LessonService lessonService;
    private final TaskService taskService;

    public AdminController(LessonService lessonService, TaskService taskService) {
        this.lessonService = lessonService;
        this.taskService = taskService;
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
}

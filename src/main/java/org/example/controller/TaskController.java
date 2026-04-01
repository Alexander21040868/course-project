package org.example.controller;

import org.example.dto.TaskDto;
import org.example.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAll(@RequestParam(required = false) String search,
                                                 Principal principal) {
        return ResponseEntity.ok(taskService.findAll(search, principal.getName()));
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<TaskDto>> getByLesson(@PathVariable Long lessonId, Principal principal) {
        return ResponseEntity.ok(taskService.findByLesson(lessonId, principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(taskService.findById(id, principal.getName()));
    }
}

package org.example.controller;

import org.example.dto.TaskDto;
import org.example.service.TaskService;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<Page<TaskDto>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            Principal principal) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(taskService.findPage(search, principal.getName(), page, safeSize));
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<TaskDto>> getByLesson(@PathVariable("lessonId") Long lessonId, Principal principal) {
        return ResponseEntity.ok(taskService.findByLesson(lessonId, principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getById(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(taskService.findById(id, principal.getName()));
    }
}

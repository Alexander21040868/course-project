package org.example.controller;

import org.example.dto.TaskDto;
import org.example.repository.TaskRepository;
import org.example.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/daily-task")
public class DailyTaskController {

    private final TaskRepository taskRepo;
    private final TaskService taskService;

    public DailyTaskController(TaskRepository taskRepo, TaskService taskService) {
        this.taskRepo = taskRepo;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<TaskDto> getDailyTask(Principal principal) {
        long total = taskRepo.count();
        if (total == 0) return ResponseEntity.noContent().build();

        long dailyIndex = LocalDate.now().toEpochDay() % total;
        var tasks = taskRepo.findAllByOrderByIdAsc();
        Long taskId = tasks.get((int) dailyIndex).getId();

        return ResponseEntity.ok(taskService.findById(taskId, principal.getName()));
    }
}

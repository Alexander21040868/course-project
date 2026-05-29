package org.example.controller;

import org.example.dto.TaskDto;
import org.example.service.DailyTaskService;
import org.example.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/daily-task")
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;
    private final TaskService taskService;

    public DailyTaskController(DailyTaskService dailyTaskService, TaskService taskService) {
        this.dailyTaskService = dailyTaskService;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<TaskDto> getDailyTask(Principal principal) {
        Long taskId = dailyTaskService.currentDailyTaskId();
        if (taskId == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(taskService.findById(taskId, principal.getName()));
    }
}

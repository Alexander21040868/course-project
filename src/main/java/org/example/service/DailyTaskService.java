package org.example.service;

import org.example.entity.Task;
import org.example.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DailyTaskService {

    private final TaskRepository taskRepo;

    public DailyTaskService(TaskRepository taskRepo) {
        this.taskRepo = taskRepo;
    }

    public Long currentDailyTaskId() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> tasks = taskRepo.findReleasedOrderByIdAsc(now);
        if (tasks.isEmpty()) return null;
        long index = Math.floorMod(LocalDate.now().toEpochDay(), tasks.size());
        return tasks.get((int) index).getId();
    }

    public boolean isDailyTask(Long taskId) {
        Long current = currentDailyTaskId();
        return current != null && current.equals(taskId);
    }
}

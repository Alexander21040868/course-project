package org.example.service;

import org.example.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DailyTaskService {

    private final TaskRepository taskRepo;

    public DailyTaskService(TaskRepository taskRepo) {
        this.taskRepo = taskRepo;
    }

    public Long currentDailyTaskId() {
        long total = taskRepo.count();
        if (total == 0) return null;
        long index = Math.floorMod(LocalDate.now().toEpochDay(), total);
        return taskRepo.findAllByOrderByIdAsc().get((int) index).getId();
    }

    public boolean isDailyTask(Long taskId) {
        Long current = currentDailyTaskId();
        return current != null && current.equals(taskId);
    }
}

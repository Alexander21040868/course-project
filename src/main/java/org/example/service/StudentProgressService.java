package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentProgressService {

    private final UserRepository userRepo;
    private final TaskRepository taskRepo;
    private final LessonRepository lessonRepo;
    private final SubmissionRepository submissionRepo;

    public StudentProgressService(UserRepository userRepo, TaskRepository taskRepo,
                                  LessonRepository lessonRepo, SubmissionRepository submissionRepo) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
    }

    public List<StudentProgressDto> listStudents() {
        long totalTasks = taskRepo.count();
        return userRepo.findByRole(Role.STUDENT).stream()
                .map(u -> {
                    long solved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                            u.getId(), SubmissionStatus.CORRECT);
                    double pct = totalTasks > 0 ? (solved * 100.0 / totalTasks) : 0;
                    return new StudentProgressDto(u.getId(), u.getUsername(),
                            u.getXp(), u.getLevel(), solved, totalTasks, Math.round(pct * 10) / 10.0);
                })
                .toList();
    }

    public StudentDetailDto getDetail(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        long totalSolved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);

        List<LessonProgressDto> lessons = lessonRepo.findAllByOrderByOrderIndexAsc().stream()
                .map(lesson -> {
                    List<Task> tasks = taskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId());
                    List<TaskProgressDto> taskProgress = tasks.stream().map(t -> {
                        boolean solved = submissionRepo.existsByUserIdAndTaskIdAndStatus(
                                user.getId(), t.getId(), SubmissionStatus.CORRECT);
                        long attempts = submissionRepo.findByUserIdAndTaskIdOrderBySubmittedAtDesc(
                                user.getId(), t.getId()).size();
                        return new TaskProgressDto(t.getId(), t.getTitle(),
                                t.getDifficulty().name(), solved, attempts);
                    }).toList();
                    int solved = (int) taskProgress.stream().filter(TaskProgressDto::solved).count();
                    return new LessonProgressDto(lesson.getId(), lesson.getTitle(),
                            solved, tasks.size(), taskProgress);
                }).toList();

        return new StudentDetailDto(
                user.getId(), user.getUsername(), user.getXp(), user.getLevel(),
                totalSolved, lessons);
    }
}

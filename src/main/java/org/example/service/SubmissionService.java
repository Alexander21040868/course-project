package org.example.service;

import org.example.dto.AchievementDto;
import org.example.dto.SubmissionRequest;
import org.example.dto.SubmissionResponse;
import org.example.entity.*;
import org.example.repository.SubmissionRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final GamificationService gamificationService;

    public SubmissionService(SubmissionRepository submissionRepo, TaskRepository taskRepo,
                             UserRepository userRepo, GamificationService gamificationService) {
        this.submissionRepo = submissionRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public SubmissionResponse submit(SubmissionRequest req, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Task task = taskRepo.findById(req.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));

        boolean alreadySolved = submissionRepo.existsByUserIdAndTaskIdAndStatus(
                user.getId(), task.getId(), SubmissionStatus.CORRECT);

        Submission sub = new Submission();
        sub.setUser(user);
        sub.setTask(task);
        sub.setCode(req.code());

        String result = checkSolution(req.code(), task);
        boolean correct = "OK".equals(result);

        sub.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.WRONG);
        sub.setOutput(correct ? "Верно! Отличная работа." : result);
        submissionRepo.save(sub);

        int xpEarned = 0;
        List<AchievementDto> newAchievements = new ArrayList<>();

        if (correct && !alreadySolved) {
            xpEarned = gamificationService.addXp(user, task.getXpReward());
            newAchievements = gamificationService.checkAndGrantAchievements(user);
        }

        return new SubmissionResponse(
                sub.getId(), sub.getStatus().name(), sub.getOutput(),
                xpEarned, newAchievements, sub.getSubmittedAt()
        );
    }

    /**
     * Простая проверка: сравниваем вывод программы с ожидаемым.
     * В реальном проекте здесь была бы компиляция и запуск C-кода в песочнице.
     */
    private String checkSolution(String code, Task task) {
        if (task.getExpectedOutput() == null || task.getExpectedOutput().isBlank()) {
            return code.isBlank() ? "Пустое решение" : "OK";
        }

        String expected = task.getExpectedOutput().trim();
        String normalizedCode = code.trim();

        if (normalizedCode.isEmpty()) {
            return "Пустое решение — напишите код!";
        }

        if (!normalizedCode.contains("main")) {
            return "Ошибка: не найдена функция main()";
        }

        if (containsExpectedLogic(normalizedCode, expected)) {
            return "OK";
        }

        return "Неверный ответ. Ожидаемый вывод: " + expected;
    }

    private boolean containsExpectedLogic(String code, String expected) {
        String cleaned = expected.replace("\\n", "\n").trim();
        for (String line : cleaned.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && code.contains(trimmed)) {
                return true;
            }
        }

        if (code.contains("printf") || code.contains("puts") || code.contains("cout")) {
            String[] keywords = expected.split("\\s+");
            int matches = 0;
            for (String kw : keywords) {
                if (code.contains(kw)) matches++;
            }
            return matches >= keywords.length / 2 + 1;
        }

        return false;
    }
}

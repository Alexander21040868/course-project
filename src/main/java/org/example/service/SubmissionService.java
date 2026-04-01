package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final TestCaseRepository testCaseRepo;
    private final GamificationService gamificationService;

    public SubmissionService(SubmissionRepository submissionRepo, TaskRepository taskRepo,
                             UserRepository userRepo, TestCaseRepository testCaseRepo,
                             GamificationService gamificationService) {
        this.submissionRepo = submissionRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.testCaseRepo = testCaseRepo;
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

        List<TestCase> testCases = testCaseRepo.findByTaskIdOrderByOrderIndexAsc(task.getId());
        List<TestResultDto> testResults = new ArrayList<>();
        int passed = 0;
        int total;

        if (!testCases.isEmpty()) {
            total = testCases.size();
            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                boolean ok = checkAgainstTest(req.code(), tc);
                if (ok) passed++;
                testResults.add(new TestResultDto(
                        i + 1, ok,
                        tc.isSample() ? tc.getInput() : null,
                        tc.isSample() ? tc.getExpectedOutput() : null,
                        tc.isSample() && !ok ? "(ваш вывод не совпадает)" : null,
                        tc.isSample()
                ));
            }
        } else {
            total = 1;
            boolean ok = checkLegacy(req.code(), task);
            if (ok) passed = 1;
            testResults.add(new TestResultDto(1, ok, null, task.getExpectedOutput(), null, true));
        }

        boolean correct = passed == total;
        sub.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.WRONG);

        String output = correct
                ? "Все тесты пройдены! Отличная работа."
                : "Пройдено " + passed + " из " + total + " тестов.";
        sub.setOutput(output);
        submissionRepo.save(sub);

        int xpEarned = 0;
        List<AchievementDto> newAchievements = new ArrayList<>();
        if (correct && !alreadySolved) {
            xpEarned = gamificationService.addXp(user, task.getXpReward());
            newAchievements = gamificationService.checkAndGrantAchievements(user);
        }

        return new SubmissionResponse(
                sub.getId(), sub.getStatus().name(), output,
                xpEarned, passed, total, testResults,
                newAchievements, sub.getSubmittedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<SubmissionHistoryDto> getTaskHistory(Long taskId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return submissionRepo.findByUserIdAndTaskIdOrderBySubmittedAtDesc(user.getId(), taskId)
                .stream()
                .map(s -> new SubmissionHistoryDto(s.getId(), s.getTask().getTitle(),
                        s.getStatus().name(), s.getOutput(), s.getSubmittedAt()))
                .toList();
    }

    private boolean checkAgainstTest(String code, TestCase tc) {
        String expected = tc.getExpectedOutput().trim();
        String normalizedCode = code.trim();

        if (normalizedCode.isEmpty() || !normalizedCode.contains("main")) {
            return false;
        }

        return containsExpectedLogic(normalizedCode, expected);
    }

    private boolean checkLegacy(String code, Task task) {
        if (task.getExpectedOutput() == null || task.getExpectedOutput().isBlank()) {
            return !code.isBlank();
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty() || !normalizedCode.contains("main")) {
            return false;
        }
        return containsExpectedLogic(normalizedCode, task.getExpectedOutput().trim());
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

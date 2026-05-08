package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final TestCaseRepository testCaseRepo;
    private final GamificationService gamificationService;
    private final DockerCExecutionService cExec;

    public SubmissionService(SubmissionRepository submissionRepo, TaskRepository taskRepo,
                             UserRepository userRepo, TestCaseRepository testCaseRepo,
                             GamificationService gamificationService,
                             DockerCExecutionService cExec) {
        this.submissionRepo = submissionRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.testCaseRepo = testCaseRepo;
        this.gamificationService = gamificationService;
        this.cExec = cExec;
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
                TestEval ev = evaluateAgainstTest(req.code(), tc);
                if (ev.passed()) {
                    passed++;
                }
                testResults.add(new TestResultDto(
                        i + 1, ev.passed(),
                        tc.isSample() ? tc.getInput() : null,
                        tc.isSample() ? tc.getExpectedOutput() : null,
                        tc.isSample() && !ev.passed() ? shorten(ev.detail(), 600) : null,
                        tc.isSample()
                ));
            }
        } else {
            total = 1;
            TestEval ev = evaluateLegacy(req.code(), task);
            if (ev.passed()) {
                passed = 1;
            }
            testResults.add(new TestResultDto(
                    1, ev.passed(), null, task.getExpectedOutput(),
                    !ev.passed() ? shorten(ev.detail(), 600) : null, true));
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
        if (correct) {
            gamificationService.updateStreak(user);
            if (!alreadySolved) {
                xpEarned = gamificationService.addXp(user, task.getXpReward());
                newAchievements = gamificationService.checkAndGrantAchievements(user);
            }
        }

        log.info("User {} submitted task #{}: {} ({}/{} tests) +{} XP",
                username, req.taskId(), sub.getStatus(), passed, total, xpEarned);

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
                        s.getStatus().name(), s.getOutput(), s.getCode(), s.getSubmittedAt()))
                .toList();
    }

    private record TestEval(boolean passed, String detail) {}

    private TestEval evaluateAgainstTest(String code, TestCase tc) {
        String expectedRaw = tc.getExpectedOutput();
        if (expectedRaw == null || expectedRaw.isBlank()) {
            return new TestEval(false, "У теста пустой ожидаемый вывод.");
        }
        String c = code == null ? "" : code.trim();
        if (c.isEmpty() || !c.contains("main")) {
            return new TestEval(false, "Нужен непустой код с функцией main.");
        }

        String expected = normalizeExpected(expectedRaw);
        String stdin = tc.getInput() == null ? "" : tc.getInput();
        CExecutionResult r = cExec.compileAndRun(c, stdin);
        return mapExecutionToEval(r, expected);
    }

    private TestEval evaluateLegacy(String code, Task task) {
        String expectedRaw = task.getExpectedOutput();
        if (expectedRaw == null || expectedRaw.isBlank()) {
            return new TestEval(false, "Нет тест-кейсов и не задан expected output у задачи.");
        }
        String c = code == null ? "" : code.trim();
        if (c.isEmpty() || !c.contains("main")) {
            return new TestEval(false, "Нужен непустой код с функцией main.");
        }

        String expected = normalizeExpected(expectedRaw);
        CExecutionResult r = cExec.compileAndRun(c, "");
        return mapExecutionToEval(r, expected);
    }

    private static TestEval mapExecutionToEval(CExecutionResult r, String expected) {
        return switch (r.kind()) {
            case DISABLED, DOCKER_ERROR -> new TestEval(false, r.stderrOrCompileLog());
            case COMPILE_ERROR -> new TestEval(false, "Компиляция:\n" + shorten(r.stderrOrCompileLog(), 1200));
            case TIMEOUT -> new TestEval(false, r.stderrOrCompileLog());
            case RUNTIME_ERROR -> new TestEval(false,
                    "Ошибка выполнения (код/сигнал). stderr:\n"
                            + shorten(r.stderrOrCompileLog(), 600)
                            + "\nstdout:\n" + shorten(r.stdout(), 400));
            case OK -> {
                if (normalizedMatch(r.stdout(), expected)) {
                    yield new TestEval(true, null);
                }
                yield new TestEval(false,
                        "Получено:\n" + normalizeOut(r.stdout()) + "\nОжидалось:\n" + normalizeOut(expected));
            }
        };
    }

    private static String normalizeExpected(String s) {
        return s.replace("\\n", "\n").replace("\\t", "\t");
    }

    private static String normalizeOut(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private static boolean normalizedMatch(String actual, String expected) {
        String a = normalizeOut(actual);
        String e = normalizeOut(expected);
        if (a.equals(e)) {
            return true;
        }
        return normalizeLines(a).equals(normalizeLines(e));
    }

    private static String normalizeLines(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Arrays.stream(s.split("\n", -1))
                .map(String::strip)
                .collect(Collectors.joining("\n"))
                .strip();
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}

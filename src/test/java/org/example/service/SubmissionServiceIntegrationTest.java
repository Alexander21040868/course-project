package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.SubmissionRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.entity.SubmissionStatus;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.UserRepository;
import org.example.support.TestId;
import org.example.testsupport.ControllableSandboxDockerService;
import org.example.testsupport.TestSandboxConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestSandboxConfig.class)
class SubmissionServiceIntegrationTest {

    @Autowired
    AuthService authService;
    @Autowired
    TaskService taskService;
    @Autowired
    SubmissionService submissionService;
    @Autowired
    UserRepository userRepo;
    @Autowired
    StudentTeacherRepository studentTeacherRepo;
    @Autowired
    ControllableSandboxDockerService sandbox;

    private String teacherUser;
    private String studentUser;
    private Long taskId;

    @BeforeEach
    void setUp() {
        teacherUser = TestId.uniq("t");
        studentUser = TestId.uniq("s");
        authService.register(new AuthRequest(teacherUser, "p1", teacherUser + "@t.dev", "TEACHER"));
        authService.register(new AuthRequest(studentUser, "p2", studentUser + "@t.dev", "STUDENT"));

        long tid = userRepo.findByUsername(teacherUser).orElseThrow().getId();
        long sid = userRepo.findByUsername(studentUser).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        var req = new TaskCreateRequest(
                "Add two numbers",
                "demo",
                "EASY",
                15,
                "#include <stdio.h>\nint main(){return 0;}",
                "3",
                null,
                null,
                null
        );
        taskId = taskService.create(req, teacherUser).id();

        sandbox.setHandler((code, stdin) -> {
            if (stdin != null && stdin.contains("1")) {
                return new CExecutionResult(CExecutionResult.Kind.OK, "2\n", "", ExecutionBackend.LOCAL_GCC);
            }
            return new CExecutionResult(CExecutionResult.Kind.OK, "3\n", "", ExecutionBackend.LOCAL_GCC);
        });
    }

    @Test
    void correctLegacyTaskGrantsXpOnce() {
        String code = "#include <stdio.h>\nint main(){printf(\"x\");return 0;}\n";

        var r1 = submissionService.submit(new SubmissionRequest(taskId, code), studentUser);
        assertEquals(SubmissionStatus.CORRECT.name(), r1.status());
        assertTrue(r1.xpEarned() > 0);
        assertEquals(1, r1.passedTests());
        assertEquals(1, r1.totalTests());

        var r2 = submissionService.submit(new SubmissionRequest(taskId, code), studentUser);
        assertEquals(SubmissionStatus.CORRECT.name(), r2.status());
        assertEquals(0, r2.xpEarned());
    }

    @Test
    void wrongOutputNoXp() {
        sandbox.setHandler((c, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "nope\n", "", ExecutionBackend.LOCAL_GCC));

        var r = submissionService.submit(
                new SubmissionRequest(taskId, "#include <stdio.h>\nint main(){return 0;}\n"),
                studentUser);
        assertEquals(SubmissionStatus.WRONG.name(), r.status());
        assertEquals(0, r.xpEarned());
    }

    @Test
    void taskWithTestCases_allMustPass() {
        var multi = new TaskCreateRequest(
                "Echo",
                null,
                "EASY",
                10,
                null,
                null,
                null,
                java.util.List.of(
                        new TaskCreateRequest.TestCaseInput("1", "2", true),
                        new TaskCreateRequest.TestCaseInput("2", "4", true)
                ),
                null
        );
        Long multiTaskId = taskService.create(multi, teacherUser).id();

        sandbox.setHandler((code, in) -> {
            if (in != null && in.strip().startsWith("1")) {
                return new CExecutionResult(CExecutionResult.Kind.OK, "2\n", "", ExecutionBackend.LOCAL_GCC);
            }
            if (in != null && in.strip().startsWith("2")) {
                return new CExecutionResult(CExecutionResult.Kind.OK, "4\n", "", ExecutionBackend.LOCAL_GCC);
            }
            return new CExecutionResult(CExecutionResult.Kind.OK, "\n", "", ExecutionBackend.LOCAL_GCC);
        });

        var ok = submissionService.submit(
                new SubmissionRequest(multiTaskId, "#include <stdio.h>\nint main(){return 0;}\n"),
                studentUser);
        assertEquals(SubmissionStatus.CORRECT.name(), ok.status());
        assertEquals(2, ok.totalTests());
        assertEquals(2, ok.passedTests());

        sandbox.setHandler((code, in) -> {
            if (in != null && in.strip().startsWith("1")) {
                return new CExecutionResult(CExecutionResult.Kind.OK, "2\n", "", ExecutionBackend.LOCAL_GCC);
            }
            return new CExecutionResult(CExecutionResult.Kind.OK, "bad\n", "", ExecutionBackend.LOCAL_GCC);
        });

        var bad = submissionService.submit(
                new SubmissionRequest(multiTaskId, "#include <stdio.h>\nint main(){return 0;}\n"),
                studentUser);
        assertEquals(SubmissionStatus.WRONG.name(), bad.status());
        assertEquals(1, bad.passedTests());
        assertEquals(2, bad.totalTests());
    }
}

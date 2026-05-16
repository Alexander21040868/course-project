package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.SubmissionRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.UserRepository;
import org.example.support.TestId;
import org.example.testsupport.DockerTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
class DockerSubmissionThroughDockerIT {

    @DynamicPropertySource
    static void forceRealDocker(DynamicPropertyRegistry r) {
        r.add("app.code-execution.enabled", () -> "true");
        r.add("app.code-execution.fallback-local-gcc", () -> "false");
        r.add("app.code-execution.wall-timeout-ms", () -> 180_000);
        r.add("app.code-execution.run-timeout-sec", () -> 15);
    }

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerTestSupport.isDockerDaemonUp());
    }

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

    @Test
    void submission_legacyTask_runsInDockerContainer() {
        String teacher = TestId.uniq("dk_t");
        String student = TestId.uniq("dk_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@dock.it", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@dock.it", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(student).orElseThrow().getId(),
                userRepo.findByUsername(teacher).orElseThrow().getId()));

        long taskId = taskService.create(new TaskCreateRequest(
                "Docker IT " + teacher,
                null,
                "EASY",
                5,
                null,
                "42",
                null,
                null,
                null), teacher).id();

        var res = submissionService.submit(
                new SubmissionRequest(taskId,
                        "#include <stdio.h>\nint main(void){printf(\"42\");return 0;}\n"),
                student);

        assertEquals("CORRECT", res.status(), res.output());
        assertNotNull(res.codeSandbox());
        assertEquals("DOCKER", res.codeSandbox(), res.output());
    }
}

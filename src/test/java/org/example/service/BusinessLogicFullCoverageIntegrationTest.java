package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.example.support.TestId;
import org.example.testsupport.ControllableSandboxDockerService;
import org.example.testsupport.TestSandboxConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSandboxConfig.class)
class BusinessLogicFullCoverageIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AuthService authService;
    @Autowired
    UserRepository userRepo;
    @Autowired
    TaskRepository taskRepo;
    @Autowired
    TaskService taskService;
    @Autowired
    LessonService lessonService;
    @Autowired
    ChallengeService challengeService;
    @Autowired
    ChallengeRepository challengeRepo;
    @Autowired
    ChallengeParticipantRepository participantRepo;
    @Autowired
    SubmissionRepository submissionRepo;
    @Autowired
    SubmissionService submissionService;
    @Autowired
    ArticleService articleService;
    @Autowired
    CodeHintService codeHintService;
    @Autowired
    GroupInviteService groupInviteService;
    @Autowired
    StudyGroupService studyGroupService;
    @Autowired
    StudentProgressService studentProgressService;
    @Autowired
    StudentTeacherRepository studentTeacherRepo;
    @Autowired
    TestCaseRepository testCaseRepo;
    @Autowired
    NotificationService notificationService;
    @Autowired
    ControllableSandboxDockerService sandbox;

    private static TaskCreateRequest stdTask(String teacherUsername, String title) {
        return new TaskCreateRequest(title, null, "EASY", 5,
                null, "ok", null, null, null);
    }

    @BeforeEach
    void resetSandbox() {
        sandbox.setHandler((c, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "ok\n", "", ExecutionBackend.LOCAL_GCC));
    }

    @Test
    void groupInvite_pendingCount_unknownUser_returnsZero() {
        assertEquals(0L, groupInviteService.pendingCount("__no_user_" + TestId.uniq("x")));
    }

    @Test
    void auth_register_duplicateEmail_throws() {
        String u = TestId.uniq("ae_u");
        authService.register(new AuthRequest(u, "p", u + "@dup.dev", "STUDENT"));
        assertThrows(IllegalArgumentException.class, () ->
                authService.register(new AuthRequest(TestId.uniq("ae2"), "p", u + "@dup.dev", "STUDENT")));
    }

    @Test
    void auth_register_invalidRole_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register(new AuthRequest(TestId.uniq("badrole"), "p", "x@y.dev", "HACKER")));
    }

    @Test
    void notification_notifyUser_missingUser_noOp() {
        notificationService.notifyUser(-1L, "t", "m");
    }

    @Test
    void notification_unreadCount_unknownUser_returnsZero() {
        assertEquals(0L, notificationService.unreadCount("__ghost_" + TestId.uniq("n")));
    }

    @Test
    void lesson_findPageFor_unknownUser_emptyPage() {
        var p = lessonService.findPageFor("__nouser_" + TestId.uniq("l"), null, 0, 10);
        assertTrue(p.isEmpty());
    }

    @Test
    void lesson_findPageFor_student_twoTeachers_filterMismatch_empty() {
        String t1 = TestId.uniq("lt1");
        String t2 = TestId.uniq("lt2");
        String st = TestId.uniq("lst");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        long id1 = userRepo.findByUsername(t1).orElseThrow().getId();
        long id2 = userRepo.findByUsername(t2).orElseThrow().getId();
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, id1));
        studentTeacherRepo.save(new StudentTeacherLink(sid, id2));
        var empty = lessonService.findPageFor(st, "no_such_teacher_xyz", 0, 10);
        assertTrue(empty.isEmpty());
    }

    @Test
    void lesson_listLinkedTeachers_forTeacher_empty() {
        String te = TestId.uniq("lte");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        assertTrue(lessonService.listLinkedTeachersForStudent(te).isEmpty());
    }

    @Test
    void article_search_numericId_and_blankQuery_and_bigNumberFallback() {
        String te = TestId.uniq("art");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        long id = articleService.create(new ArticleCreateRequest("SearchMe", "b", null, 1), te).id();
        assertFalse(articleService.search(null).isEmpty());
        assertFalse(articleService.search("  ").isEmpty());
        assertEquals(1, articleService.search("#" + id).size());
        assertTrue(articleService.search("99999999999999999999999999999999999").isEmpty());
    }

    @Test
    void task_findPage_hashId_and_emptySecondPage() {
        String te = TestId.uniq("tp");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        long tid = taskService.create(stdTask(te, "HashId task" + TestId.uniq("h")), te).id();
        var p0 = taskService.findPage("#" + tid, te, 0, 10);
        assertEquals(1, p0.getTotalElements());
        var p1 = taskService.findPage("#" + tid, te, 1, 10);
        assertTrue(p1.isEmpty());
    }

    @Test
    void task_findByLesson_foreignTeacher_throws() {
        String t1 = TestId.uniq("lb1");
        String t2 = TestId.uniq("lb2");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        long lessonId = lessonService.create(new LessonCreateRequest(
                "L", "", "", 10 + Math.floorMod(t1.hashCode(), 50), List.of()), t1).id();
        assertThrows(IllegalArgumentException.class, () -> taskService.findByLesson(lessonId, t2));
    }

    @Test
    void task_delete_blockedInActiveChallenge() {
        String te = TestId.uniq("td");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(3);
        var ch = challengeService.create(new ChallengeCreateRequest("DelBlk", "", s, s.plusDays(1), 10), te);
        long taskId = taskService.create(stdTask(te, "InCh " + TestId.uniq("i")), te).id();
        challengeService.attachTask(ch.id(), taskId, te);
        assertThrows(IllegalArgumentException.class, () -> taskService.deleteForAuthor(taskId, te));
    }

    @Test
    void challenge_finalize_elo_andRated() {
        String te = TestId.uniq("fin_te");
        String s1 = TestId.uniq("fin_s1");
        String s2 = TestId.uniq("fin_s2");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(s1, "p", s1 + "@z.dev", "STUDENT"));
        authService.register(new AuthRequest(s2, "p", s2 + "@z.dev", "STUDENT"));
        User teacher = userRepo.findByUsername(te).orElseThrow();
        User u1 = userRepo.findByUsername(s1).orElseThrow();
        User u2 = userRepo.findByUsername(s2).orElseThrow();

        Task task = new Task();
        task.setTitle("Fin " + TestId.uniq("t"));
        task.setDifficulty(Difficulty.EASY);
        task.setXpReward(5);
        task.setAuthor(teacher);
        task.setExpectedOutput("1");
        taskRepo.save(task);

        Challenge ch = new Challenge();
        ch.setTitle("Past " + TestId.uniq("c"));
        ch.setDescription("");
        ch.setStartTime(LocalDateTime.now().minusDays(5));
        ch.setEndTime(LocalDateTime.now().minusDays(1));
        ch.setBonusXp(7);
        ch.setCreatedBy(teacher);
        ch.getTasks().add(task);
        challengeRepo.save(ch);

        ChallengeParticipant p1 = new ChallengeParticipant();
        p1.setChallenge(ch);
        p1.setUser(u1);
        participantRepo.save(p1);
        ChallengeParticipant p2 = new ChallengeParticipant();
        p2.setChallenge(ch);
        p2.setUser(u2);
        participantRepo.save(p2);

        Submission sub = new Submission();
        sub.setUser(u1);
        sub.setTask(task);
        sub.setCode("c");
        sub.setStatus(SubmissionStatus.CORRECT);
        sub.setOutput("ok");
        submissionRepo.save(sub);

        challengeService.finalizeChallengeById(ch.getId());
        Challenge done = challengeRepo.findDetailById(ch.getId()).orElseThrow();
        assertTrue(done.isRated());
        assertNotEquals(0, participantRepo.findByChallengeIdAndUserId(ch.getId(), u1.getId()).orElseThrow().getEloDelta());
    }

    @Test
    void challenge_finalize_singleParticipant_noEloChangeLoop() {
        String te = TestId.uniq("onep");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        User teacher = userRepo.findByUsername(te).orElseThrow();
        Task task = new Task();
        task.setTitle("One " + TestId.uniq(""));
        task.setDifficulty(Difficulty.EASY);
        task.setXpReward(3);
        task.setAuthor(teacher);
        taskRepo.save(task);
        Challenge ch = new Challenge();
        ch.setTitle("OneP");
        ch.setStartTime(LocalDateTime.now().minusDays(2));
        ch.setEndTime(LocalDateTime.now().minusDays(1));
        ch.setBonusXp(5);
        ch.setCreatedBy(teacher);
        ch.getTasks().add(task);
        challengeRepo.save(ch);
        challengeService.finalizeChallengeById(ch.getId());
        assertTrue(challengeRepo.findDetailById(ch.getId()).orElseThrow().isRated());
    }

    @Test
    void challenge_finalizeEndedUnrated_batch() {
        String te = TestId.uniq("batch");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        User teacher = userRepo.findByUsername(te).orElseThrow();
        Challenge ch = new Challenge();
        ch.setTitle("Batch " + TestId.uniq(""));
        ch.setStartTime(LocalDateTime.now().minusDays(3));
        ch.setEndTime(LocalDateTime.now().minusHours(2));
        ch.setBonusXp(1);
        ch.setCreatedBy(teacher);
        challengeRepo.save(ch);
        challengeService.finalizeEndedUnratedChallenges();
        assertTrue(challengeRepo.findDetailById(ch.getId()).orElseThrow().isRated());
    }

    @Test
    void challenge_deleteStartedWithNoTasks_removes() {
        String te = TestId.uniq("empc");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        User teacher = userRepo.findByUsername(te).orElseThrow();
        Challenge ch = new Challenge();
        ch.setTitle("Empty");
        ch.setStartTime(LocalDateTime.now().minusHours(1));
        ch.setEndTime(LocalDateTime.now().plusDays(1));
        ch.setBonusXp(1);
        ch.setCreatedBy(teacher);
        challengeRepo.save(ch);
        long id = ch.getId();
        challengeService.deleteStartedChallengesWithNoTasks();
        assertTrue(challengeRepo.findById(id).isEmpty());
    }

    @Test
    void challenge_join_errors() {
        String te = TestId.uniq("jnte");
        String st = TestId.uniq("jnst");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        LocalDateTime s = LocalDateTime.now().plusHours(10);
        var dto = challengeService.create(new ChallengeCreateRequest("Jn", "", s, s.plusDays(1), 1), te);
        assertThrows(IllegalArgumentException.class, () -> challengeService.join(dto.id(), "__nouser"));
        assertThrows(IllegalArgumentException.class, () -> challengeService.join(-1L, st));
        challengeService.join(dto.id(), st);
        assertThrows(IllegalArgumentException.class, () -> challengeService.join(dto.id(), st));
        LocalDateTime past = LocalDateTime.now().minusDays(2);
        Challenge ended = new Challenge();
        ended.setTitle("End");
        ended.setStartTime(past.minusDays(1));
        ended.setEndTime(past);
        ended.setBonusXp(1);
        ended.setCreatedBy(userRepo.findByUsername(te).orElseThrow());
        challengeRepo.save(ended);
        assertThrows(IllegalArgumentException.class, () -> challengeService.join(ended.getId(), st));
    }

    @Test
    void challenge_create_pastStart_throws() {
        String te = TestId.uniq("pst");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime t0 = LocalDateTime.now().minusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
                challengeService.create(new ChallengeCreateRequest("x", "", t0, t0.plusDays(1), 1), te));
    }

    @Test
    void challenge_attachTask_crossChallenge_and_idempotent() {
        String t1 = TestId.uniq("at1");
        String t2 = TestId.uniq("at2");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(6);
        var ch1 = challengeService.create(new ChallengeCreateRequest("A1", "", s, s.plusDays(1), 1), t1);
        var ch2 = challengeService.create(new ChallengeCreateRequest("A2", "", s, s.plusDays(1), 1), t2);
        long task1 = taskService.create(stdTask(t1, "a1t"), t1).id();
        challengeService.attachTask(ch1.id(), task1, t1);
        challengeService.attachTask(ch1.id(), task1, t1);
        assertThrows(IllegalArgumentException.class, () -> challengeService.attachTask(ch1.id(), task1, t2));
        assertThrows(IllegalArgumentException.class, () -> challengeService.attachTask(ch2.id(), task1, t2));
    }

    @Test
    void challenge_attach_afterStart_throws() {
        String te = TestId.uniq("asst");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(5);
        var ch = challengeService.create(new ChallengeCreateRequest("Ast", "", s, s.plusDays(1), 1), te);
        long tid = taskService.create(stdTask(te, "ast"), te).id();
        challengeService.attachTask(ch.id(), tid, te);
        Challenge c = challengeRepo.findById(ch.id()).orElseThrow();
        c.setStartTime(LocalDateTime.now().minusMinutes(1));
        challengeRepo.save(c);
        long tid2 = taskService.create(stdTask(te, "ast2"), te).id();
        assertThrows(IllegalArgumentException.class, () -> challengeService.attachTask(ch.id(), tid2, te));
    }

    @Test
    void challenge_attach_ended_throws() {
        String te = TestId.uniq("aen");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(5);
        var ch = challengeService.create(new ChallengeCreateRequest("Aen", "", s, s.plusDays(1), 1), te);
        Challenge c = challengeRepo.findById(ch.id()).orElseThrow();
        c.setEndTime(LocalDateTime.now().minusMinutes(1));
        challengeRepo.save(c);
        long tid = taskService.create(stdTask(te, "aen2"), te).id();
        assertThrows(IllegalArgumentException.class, () -> challengeService.attachTask(ch.id(), tid, te));
    }

    @Test
    void challenge_ensureEligible_solvedTaskWithoutCvf_throws() {
        String te = TestId.uniq("elig");
        String st = TestId.uniq("elgs");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, userRepo.findByUsername(te).orElseThrow().getId()));
        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.OK, "ok\n", "", ExecutionBackend.LOCAL_GCC));
        long tid = taskService.create(stdTask(te, "elg"), te).id();
        Task t = taskRepo.findById(tid).orElseThrow();
        t.setExpectedOutput("ok");
        taskRepo.save(t);
        submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){printf(\"ok\");return 0;}\n"), st);
        LocalDateTime s = LocalDateTime.now().plusHours(4);
        var ch = challengeService.create(new ChallengeCreateRequest("El", "", s, s.plusDays(1), 1), te);
        assertThrows(IllegalArgumentException.class, () -> challengeService.attachTask(ch.id(), tid, te));
    }

    @Test
    void challenge_cancel_notOrganizer_forbidden() {
        String t1 = TestId.uniq("cn1");
        String t2 = TestId.uniq("cn2");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(2);
        var ch = challengeService.create(new ChallengeCreateRequest("Cn", "", s, s.plusDays(1), 1), t1);
        assertThrows(org.example.exception.ForbiddenOperationException.class,
                () -> challengeService.cancelBeforeStart(ch.id(), t2));
    }

    @Test
    void challenge_cancel_afterStart_throws() {
        String te = TestId.uniq("caf");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime s = LocalDateTime.now().plusHours(2);
        var ch = challengeService.create(new ChallengeCreateRequest("Caf", "", s, s.plusDays(1), 1), te);
        Challenge c = challengeRepo.findById(ch.id()).orElseThrow();
        c.setStartTime(LocalDateTime.now().minusMinutes(1));
        challengeRepo.save(c);
        assertThrows(IllegalArgumentException.class, () -> challengeService.cancelBeforeStart(ch.id(), te));
    }

    @Test
    void challenge_getResults_beforeEnd_usesUnratedDeltasBranch() {
        String te = TestId.uniq("gr");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        LocalDateTime start = LocalDateTime.now().plusHours(4);
        var ch = challengeService.create(new ChallengeCreateRequest("GR", "", start, start.plusDays(1), 3), te);
        assertNotNull(challengeService.getResults(ch.id()));
    }

    @Test
    void challenge_active_list_viaApi() throws Exception {
        String st = TestId.uniq("arena");
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        String tok = authService.login(new AuthRequest(st, "p", null, null)).token();
        mockMvc.perform(get("/api/challenges").header("Authorization", "Bearer " + tok))
                .andExpect(status().isOk());
    }

    @Test
    void submission_testCase_emptyExpected_fails() {
        String te = TestId.uniq("ece");
        String st = TestId.uniq("ecs");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(st).orElseThrow().getId(),
                userRepo.findByUsername(te).orElseThrow().getId()));
        long tid = taskService.create(stdTask(te, "EmptyOut"), te).id();
        TestCase tc = new TestCase();
        tc.setTask(taskRepo.findById(tid).orElseThrow());
        tc.setInput("");
        tc.setExpectedOutput("");
        tc.setSample(true);
        tc.setOrderIndex(0);
        testCaseRepo.save(tc);
        var res = submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st);
        assertEquals("WRONG", res.status());
    }

    @Test
    void submission_executionKinds_viaStub() {
        String te = TestId.uniq("ek");
        String st = TestId.uniq("eks");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(st).orElseThrow().getId(),
                userRepo.findByUsername(te).orElseThrow().getId()));
        long tid = taskService.create(stdTask(te, "Kinds"), te).id();
        TestCase tc = new TestCase();
        tc.setTask(taskRepo.findById(tid).orElseThrow());
        tc.setInput("");
        tc.setExpectedOutput("want");
        tc.setSample(false);
        tc.setOrderIndex(0);
        testCaseRepo.save(tc);

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.COMPILE_ERROR, "", "err", ExecutionBackend.LOCAL_GCC));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.TIMEOUT, "", "t", ExecutionBackend.LOCAL_GCC));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.RUNTIME_ERROR, "", "r", ExecutionBackend.LOCAL_GCC));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.OK, "wrong\n", "", ExecutionBackend.LOCAL_GCC));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.DISABLED, "", "off", ExecutionBackend.DISABLED));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        sandbox.setHandler((c, i) -> new CExecutionResult(CExecutionResult.Kind.DOCKER_ERROR, "", "d", ExecutionBackend.DOCKER_RUN_FAILED));
        assertEquals("WRONG", submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        resetSandbox();
    }

    @Test
    void submission_getTaskHistory_and_legacyBlankExpected() {
        String te = TestId.uniq("hist");
        String st = TestId.uniq("hst");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(st).orElseThrow().getId(),
                userRepo.findByUsername(te).orElseThrow().getId()));
        long tid = taskService.create(stdTask(te, "Hist"), te).id();
        Task t1 = taskRepo.findById(tid).orElseThrow();
        t1.setExpectedOutput("");
        taskRepo.save(t1);
        assertEquals("WRONG", submissionService.submit(
                new SubmissionRequest(tid, "#include <stdio.h>\nint main(){return 0;}\n"), st).status());

        t1.setExpectedOutput("ok");
        taskRepo.save(t1);
        submissionService.submit(new SubmissionRequest(tid,
                "#include <stdio.h>\nint main(){printf(\"ok\");return 0;}\n"), st);
        assertFalse(submissionService.getTaskHistory(tid, st).isEmpty());
    }

    @Test
    void codeHint_branches() {
        String te = TestId.uniq("hint");
        String st = TestId.uniq("hnt");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(st).orElseThrow().getId(),
                userRepo.findByUsername(te).orElseThrow().getId()));
        long tid = taskService.create(new TaskCreateRequest(
                "HintT", "Используйте scanf и цикл for", "EASY", 1,
                null, "o", "запасная подсказка", List.of(), null), te).id();
        assertFalse(codeHintService.hint(new HintRequest(tid,
                "printf(\"hi\")", "тест неверно"), st).hint().isBlank());
        assertFalse(codeHintService.hint(new HintRequest(tid, "int main(){return 0;}", ""), st).hint().isBlank());
    }

    @Test
    void studentProgress_removeFromGroup_secondTeacher_selected() {
        String t1 = TestId.uniq("rm1");
        String t2 = TestId.uniq("rm2");
        String st = TestId.uniq("rms");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(sid, null, t1);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);
        studentTeacherRepo.save(new StudentTeacherLink(sid, userRepo.findByUsername(t2).orElseThrow().getId()));
        studentProgressService.removeFromGroup(sid, t1);
        User again = userRepo.findById(sid).orElseThrow();
        assertNotNull(again.getTeacher());
    }

    @Test
    void studentProgress_dungeon_badGroupThrows() {
        String te = TestId.uniq("dung");
        String st = TestId.uniq("dst");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(sid, null, te);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);
        int ord = 50 + Math.floorMod(te.hashCode(), 40);
        lessonService.create(new LessonCreateRequest("D", "", "", ord, List.of()), te);
        var g = studyGroupService.create(te, "G" + TestId.uniq(""));
        studyGroupService.moveStudent(sid, g.id(), te);
        studentProgressService.dungeonProgressSheet(ord, g.id(), false, te);
        assertThrows(IllegalArgumentException.class, () ->
                studentProgressService.dungeonProgressSheet(ord, -1L, false, te));
        studentProgressService.dungeonProgressSheet(ord, null, true, te);
    }

    @Test
    void admin_profile_groupInvites_and_catalogPaging() throws Exception {
        String te = TestId.uniq("adm");
        String st = TestId.uniq("ads");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(sid, null, te);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);
        String tokT = authService.login(new AuthRequest(te, "p", null, null)).token();
        String tokS = authService.login(new AuthRequest(st, "p", null, null)).token();
        long taskId = taskService.create(stdTask(te, "AdmTsk"), te).id();
        int lord = 400 + Math.floorMod(te.hashCode(), 80);
        long lesId = lessonService.create(new LessonCreateRequest("LA", "", "", lord, List.of(taskId)), te).id();

        mockMvc.perform(get("/api/profile/my-teachers").header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/tasks/" + taskId).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/lessons/by-order/" + lord).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/challenges").header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/students").header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/students/available").param("search", "ads")
                        .header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/students/" + sid).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/dungeons/" + lord + "/progress")
                        .header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/groups").header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());

        long aid = articleService.create(new ArticleCreateRequest("A", "b", null, 1), te).id();
        mockMvc.perform(get("/api/admin/articles/" + aid).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/lessons/" + lesId + "/tasks")
                        .header("Authorization", "Bearer " + tokT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskIds\":[" + taskId + "]}"))
                .andExpect(status().isOk());

        var g2 = studyGroupService.create(te, "InviteG" + TestId.uniq(""));
        String st2 = TestId.uniq("st2");
        authService.register(new AuthRequest(st2, "p", st2 + "@z.dev", "STUDENT"));
        long sid2 = userRepo.findByUsername(st2).orElseThrow().getId();
        mockMvc.perform(post("/api/admin/students/" + sid2 + "/invite")
                        .header("Authorization", "Bearer " + tokT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": " + g2.id() + "}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/group-invites/incoming").header("Authorization", "Bearer "
                        + authService.login(new AuthRequest(st2, "p", null, null)).token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/group-invites/incoming/count").header("Authorization", "Bearer "
                        + authService.login(new AuthRequest(st2, "p", null, null)).token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lessons").param("page", "0").param("size", "0")
                        .header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks").param("size", "500")
                        .header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());

        long chResId = challengeService.create(new ChallengeCreateRequest(
                "R", "", LocalDateTime.now().plusHours(12), LocalDateTime.now().plusDays(1), 1), te).id();
        mockMvc.perform(get("/api/challenges/" + chResId + "/results")
                        .header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("__missing_user__", "pw", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extendedAdmin_lessonValidation_profileLegacyTeacher_challengeFindActive() throws Exception {
        String te = TestId.uniq("ex_te");
        String st = TestId.uniq("ex_st");
        authService.register(new AuthRequest(te, "p", te + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p", st + "@z.dev", "STUDENT"));
        User teacher = userRepo.findByUsername(te).orElseThrow();
        User student = userRepo.findByUsername(st).orElseThrow();
        student.setTeacher(teacher);
        userRepo.save(student);

        String tokT = authService.login(new AuthRequest(te, "p", null, null)).token();
        String tokS = authService.login(new AuthRequest(st, "p", null, null)).token();

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());

        assertThrows(IllegalArgumentException.class, () ->
                lessonService.create(new LessonCreateRequest("Neg", "", "", -1, List.of()), te));

        int ordBase = 700 + Math.floorMod(te.hashCode(), 50);
        lessonService.create(new LessonCreateRequest("O1", "", "", ordBase, List.of()), te);
        assertThrows(IllegalArgumentException.class, () ->
                lessonService.create(new LessonCreateRequest("O2", "", "", ordBase, List.of()), te));

        long tid = taskService.create(stdTask(te, "Del task"), te).id();
        mockMvc.perform(delete("/api/admin/tasks/" + tid).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isNoContent());

        long aid = articleService.create(new ArticleCreateRequest("DelA", "x", "c", 1), te).id();
        mockMvc.perform(put("/api/admin/articles/" + aid)
                        .header("Authorization", "Bearer " + tokT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ArticleCreateRequest("UpdA", "body", "c2", 2))))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/articles/" + aid).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isNoContent());

        var grp = studyGroupService.create(te, "GX" + TestId.uniq(""));
        long stM = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(stM, null, te);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);
        mockMvc.perform(put("/api/admin/students/" + stM + "/group")
                        .header("Authorization", "Bearer " + tokT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": " + grp.id() + "}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/groups/" + grp.id()).header("Authorization", "Bearer " + tokT))
                .andExpect(status().isNoContent());

        Challenge arena = new Challenge();
        arena.setTitle("Live");
        arena.setStartTime(LocalDateTime.now().minusHours(1));
        arena.setEndTime(LocalDateTime.now().plusDays(1));
        arena.setBonusXp(3);
        arena.setCreatedBy(teacher);
        challengeRepo.save(arena);
        assertFalse(challengeService.findActive(st).isEmpty());

        mockMvc.perform(get("/api/tasks/" + taskService.create(stdTask(te, "Pub"), te).id())
                        .header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lessons/" + lessonService.create(
                        new LessonCreateRequest("lid", "", "", ordBase + 3, List.of()), te).id())
                        .header("Authorization", "Bearer " + tokS))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @Rollback
    void dailyTask_endpoint_noContentWhenNothingReleased() throws Exception {
        LocalDateTime far = LocalDateTime.now().plusYears(50);
        for (Task t : taskRepo.findAll()) {
            t.setCatalogVisibleFrom(far);
            taskRepo.save(t);
        }
        String u = TestId.uniq("daily204");
        authService.register(new AuthRequest(u, "p", u + "@z.dev", "STUDENT"));
        String tok = authService.login(new AuthRequest(u, "p", null, null)).token();
        mockMvc.perform(get("/api/daily-task").header("Authorization", "Bearer " + tok))
                .andExpect(status().isNoContent());
    }
}

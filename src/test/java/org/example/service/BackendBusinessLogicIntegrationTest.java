package org.example.service;

import org.example.dto.ArticleCreateRequest;
import org.example.dto.AuthRequest;
import org.example.dto.ChallengeCreateRequest;
import org.example.dto.LessonCreateRequest;
import org.example.dto.SubmissionRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.exception.ForbiddenOperationException;
import org.example.exception.NotFoundException;
import org.example.repository.ChallengeRepository;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.UserRepository;
import org.example.support.TestId;
import org.example.testsupport.ControllableSandboxDockerService;
import org.example.testsupport.TestSandboxConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestSandboxConfig.class)
class BackendBusinessLogicIntegrationTest {

    @Autowired
    AuthService authService;
    @Autowired
    LessonService lessonService;
    @Autowired
    TaskService taskService;
    @Autowired
    SubmissionService submissionService;
    @Autowired
    ChallengeService challengeService;
    @Autowired
    ChallengeRepository challengeRepository;
    @Autowired
    StudyGroupService studyGroupService;
    @Autowired
    GroupInviteService groupInviteService;
    @Autowired
    StudentProgressService studentProgressService;
    @Autowired
    ArticleService articleService;
    @Autowired
    GamificationService gamificationService;
    @Autowired
    DailyTaskService dailyTaskService;
    @Autowired
    UserRepository userRepo;
    @Autowired
    StudentTeacherRepository studentTeacherRepo;
    @Autowired
    ControllableSandboxDockerService sandbox;

    @Test
    void studyGroup_createListDuplicateDelete() {
        String t = TestId.uniq("sg_t");
        authService.register(new AuthRequest(t, "p", t + "@x.dev", "TEACHER"));

        var g = studyGroupService.create(t, "Group " + TestId.uniq("n"));
        assertTrue(g.id() > 0);
        assertEquals(1, studyGroupService.listForTeacher(t).stream().filter(x -> x.id().equals(g.id())).count());

        assertThrows(IllegalArgumentException.class,
                () -> studyGroupService.create(t, g.name()));

        studyGroupService.delete(g.id(), t);
        assertTrue(studyGroupService.listForTeacher(t).stream().noneMatch(x -> x.id().equals(g.id())));
    }

    @Test
    void groupInvite_acceptAndDecline() {
        String te = TestId.uniq("gi_te");
        String st = TestId.uniq("gi_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();

        groupInviteService.invite(sid, null, te);
        assertFalse(groupInviteService.incoming(st).isEmpty());
        assertTrue(groupInviteService.pendingCount(st) >= 1);

        long invId = groupInviteService.incoming(st).get(0).id();
        groupInviteService.accept(invId, st);
        assertTrue(studentTeacherRepo.existsByStudentIdAndTeacherId(sid, userRepo.findByUsername(te).orElseThrow().getId()));

        String te2 = TestId.uniq("gi_te2");
        String st2 = TestId.uniq("gi_st2");
        authService.register(new AuthRequest(te2, "p1", te2 + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st2, "p2", st2 + "@x.dev", "STUDENT"));
        long sid2 = userRepo.findByUsername(st2).orElseThrow().getId();
        groupInviteService.invite(sid2, null, te2);
        long inv2 = groupInviteService.incoming(st2).get(0).id();
        groupInviteService.decline(inv2, st2);
        assertFalse(studentTeacherRepo.existsByStudentIdAndTeacherId(sid2, userRepo.findByUsername(te2).orElseThrow().getId()));
    }

    @Test
    void groupInvite_inviteWhenLinkedThrows() {
        String te = TestId.uniq("gi3_te");
        String st = TestId.uniq("gi3_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        long tid = userRepo.findByUsername(te).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        assertThrows(IllegalArgumentException.class, () -> groupInviteService.invite(sid, null, te));
    }

    @Test
    void groupInvite_doubleInviteThrows() {
        String te = TestId.uniq("gi4_te");
        String st = TestId.uniq("gi4_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();

        groupInviteService.invite(sid, null, te);
        assertThrows(IllegalArgumentException.class, () -> groupInviteService.invite(sid, null, te));
    }

    @Test
    void studentProgress_listGroup_getDetail_dungeonSheet_removeFromGroup() {
        String te = TestId.uniq("pr_te");
        String st = TestId.uniq("pr_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(sid, null, te);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);

        assertEquals(1, studentProgressService.listGroup(te).size());
        assertFalse(studentProgressService.listAvailable(te, null).stream()
                .anyMatch(s -> s.userId().equals(sid)));

        long taskId = taskService.create(new TaskCreateRequest(
                "P1", null, "EASY", 5, null, "z", null, null, null), te).id();
        int ord = 600 + Math.floorMod(te.hashCode(), 200);
        long lessonId = lessonService.create(new LessonCreateRequest(
                "Lpr", "", "", ord, List.of(taskId)), te).id();

        var detail = studentProgressService.getDetail(sid, te);
        assertFalse(detail.lessons().isEmpty());

        var sheet = studentProgressService.dungeonProgressSheet(ord, null, false, te);
        assertEquals(ord, sheet.orderIndex());
        assertFalse(sheet.tasks().isEmpty());

        studentProgressService.removeFromGroup(sid, te);
        assertFalse(studentTeacherRepo.existsByStudentIdAndTeacherId(sid, userRepo.findByUsername(te).orElseThrow().getId()));
    }

    @Test
    void studyGroup_moveStudentInAndOut() {
        String te = TestId.uniq("mv_te");
        String st = TestId.uniq("mv_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        groupInviteService.invite(sid, null, te);
        groupInviteService.accept(groupInviteService.incoming(st).get(0).id(), st);

        var g = studyGroupService.create(te, "SG " + TestId.uniq("x"));
        studyGroupService.moveStudent(sid, g.id(), te);
        assertEquals(g.id(), userRepo.findById(sid).orElseThrow().getStudyGroup().getId());

        studyGroupService.moveStudent(sid, null, te);
        assertNull(userRepo.findById(sid).orElseThrow().getStudyGroup());
    }

    @Test
    void lessonUpdateAndDelete() {
        String te = TestId.uniq("les_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        int ord = 800 + Math.floorMod(te.hashCode(), 100);
        var created = lessonService.create(new LessonCreateRequest("A", "", "", ord, List.of()), te);
        var updated = lessonService.updateForTeacher(created.id(),
                new LessonCreateRequest("B", "d", "c", ord, List.of()), te);
        assertEquals("B", updated.title());

        lessonService.deleteByIdForTeacher(created.id(), te);
        assertThrows(IllegalArgumentException.class, () -> lessonService.findById(created.id(), te));
    }

    @Test
    void taskUpdateAndDelete() {
        String te = TestId.uniq("tsk_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        long id = taskService.create(new TaskCreateRequest(
                "V1", null, "EASY", 5, null, "o", null, null, null), te).id();
        var upd = taskService.update(id, new TaskCreateRequest(
                "V2", "x", "MEDIUM", 8, null, "o", null, null, null), te);
        assertEquals("V2", upd.title());

        taskService.deleteForAuthor(id, te);
        assertThrows(IllegalArgumentException.class, () -> taskService.findById(id, te));
    }

    @Test
    void challenge_cancelBeforeStartRemoves() {
        String te = TestId.uniq("ch_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        var dto = challengeService.create(new ChallengeCreateRequest(
                "C " + TestId.uniq("x"), "d", start, start.plusDays(1), 20), te);
        long cid = dto.id();
        challengeService.cancelBeforeStart(cid, te);
        assertTrue(challengeRepository.findById(cid).isEmpty());
    }

    @Test
    void gamification_firstBloodAfterCorrectSubmission() {
        String te = TestId.uniq("gm_te");
        String st = TestId.uniq("gm_st");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        long tidTeacher = userRepo.findByUsername(te).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tidTeacher));

        sandbox.setHandler((c, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "7\n", "", ExecutionBackend.LOCAL_GCC));

        long taskId = taskService.create(new TaskCreateRequest(
                "Gmx", null, "EASY", 10, null, "7", null, null, null), te).id();

        submissionService.submit(new SubmissionRequest(taskId, "#include <stdio.h>\nint main(){return 0;}\n"), st);

        var ach = gamificationService.getUserAchievements(sid);
        assertTrue(ach.stream().anyMatch(a -> "FIRST_BLOOD".equals(a.code())));
    }

    @Test
    void dailyTask_whenReleasedTasks_returnsOptionalId() {
        String te = TestId.uniq("dt_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        taskService.create(new TaskCreateRequest(
                "Rel " + TestId.uniq("r"), null, "EASY", 1, null, "x", null, null, null), te);
        Long id = dailyTaskService.currentDailyTaskId();
        assertNotNull(id);
    }

    @Test
    void article_delete_andForeignAuthorCannotEdit() {
        String t1 = TestId.uniq("a1");
        String t2 = TestId.uniq("a2");
        authService.register(new AuthRequest(t1, "p", t1 + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@x.dev", "TEACHER"));
        long id = articleService.create(new ArticleCreateRequest("T", "b", null, 1), t1).id();

        assertThrows(ForbiddenOperationException.class, () -> articleService.findForEdit(id, t2));

        articleService.delete(id, t1);
        assertThrows(NotFoundException.class, () -> articleService.findById(id));
    }

    @Test
    void lesson_updateByOrder_and_deleteByOrder() {
        String te = TestId.uniq("ord_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        int ord = 900 + Math.floorMod(te.hashCode(), 50);
        var l = lessonService.create(new LessonCreateRequest("O1", "", "", ord, List.of()), te);
        var u = lessonService.updateByOrderForTeacher(ord,
                new LessonCreateRequest("O2", "d", "c", ord + 1, List.of()), te);
        assertEquals("O2", u.title());
        assertEquals(ord + 1, u.orderIndex());

        lessonService.deleteByOrderForTeacher(ord + 1, te);
        assertThrows(IllegalArgumentException.class, () -> lessonService.findById(l.id(), te));
    }

    @Test
    void challenge_create_withEndBeforeStart_throws() {
        String te = TestId.uniq("iv_ch");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        LocalDateTime t0 = LocalDateTime.now().plusHours(2);
        assertThrows(IllegalArgumentException.class, () -> challengeService.create(
                new ChallengeCreateRequest("bad", "", t0, t0.minusHours(1), 1), te));
    }

    @Test
    void studyGroup_emptyName_throws() {
        String te = TestId.uniq("eg_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        assertThrows(IllegalArgumentException.class, () -> studyGroupService.create(te, "   "));
    }

    @Test
    void studentProgress_getDetail_notMyStudent_throws() {
        String te = TestId.uniq("det_te");
        String st = TestId.uniq("det_st");
        String te2 = TestId.uniq("det_te2");
        authService.register(new AuthRequest(te, "p1", te + "@x.dev", "TEACHER"));
        authService.register(new AuthRequest(st, "p2", st + "@x.dev", "STUDENT"));
        authService.register(new AuthRequest(te2, "p3", te2 + "@x.dev", "TEACHER"));
        long sid = userRepo.findByUsername(st).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, userRepo.findByUsername(te).orElseThrow().getId()));

        assertThrows(IllegalArgumentException.class, () -> studentProgressService.getDetail(sid, te2));
    }

    @Test
    void challenge_getResults_returnsList() {
        String te = TestId.uniq("res_te");
        authService.register(new AuthRequest(te, "p", te + "@x.dev", "TEACHER"));
        LocalDateTime start = LocalDateTime.now().plusHours(8);
        var ch = challengeService.create(new ChallengeCreateRequest(
                "Res " + TestId.uniq("r"), "", start, start.plusDays(1), 10), te);
        assertNotNull(challengeService.getResults(ch.id()));
    }
}

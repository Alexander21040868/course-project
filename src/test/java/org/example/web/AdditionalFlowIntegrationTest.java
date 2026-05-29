package org.example.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ArticleCreateRequest;
import org.example.dto.AuthRequest;
import org.example.dto.ChallengeCreateRequest;
import org.example.dto.HintRequest;
import org.example.dto.LessonCreateRequest;
import org.example.dto.SubmissionRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.CExecutionResult;
import org.example.service.ExecutionBackend;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSandboxConfig.class)
class AdditionalFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AuthService authService;
    @Autowired
    UserRepository userRepo;
    @Autowired
    StudentTeacherRepository studentTeacherRepo;
    @Autowired
    ControllableSandboxDockerService sandbox;

    @BeforeEach
    void resetSandbox() {
        sandbox.setHandler((code, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "42\n", "", ExecutionBackend.LOCAL_GCC));
    }

    @Test
    void leaderboard_xpAndRating_sortParamsReturn200() throws Exception {
        String u = TestId.uniq("lb");
        authService.register(new AuthRequest(u, "p", u + "@z.dev", "STUDENT"));
        String token = authService.login(new AuthRequest(u, "p", null, null)).token();

        mockMvc.perform(get("/api/leaderboard").param("sort", "xp")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/leaderboard").param("sort", "rating")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void notifications_unreadThenMarkRead() throws Exception {
        String teacher = TestId.uniq("n_t");
        String student = TestId.uniq("n_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));
        long tid = userRepo.findByUsername(teacher).orElseThrow().getId();
        long sid = userRepo.findByUsername(student).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        int ord = 200 + Math.floorMod(teacher.hashCode(), 300);
        mockMvc.perform(post("/api/admin/lessons")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LessonCreateRequest("Echo dungeon", "", "", ord, List.of()))))
                .andExpect(status().isOk());

        String listJson = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = objectMapper.readTree(listJson);
        assertTrue(arr.isArray() && !arr.isEmpty());

        long notifId = arr.get(0).get("id").asLong();
        mockMvc.perform(put("/api/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/count")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk());
    }

    @Test
    void taskCatalog_searchByTitle() throws Exception {
        String teacher = TestId.uniq("cat_t");
        String student = TestId.uniq("cat_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        String marker = "UniqueCatMarker_" + TestId.uniq("m");
        mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                marker, null, "EASY", 5, null, "x", null, null, null))))
                .andExpect(status().isOk());

        String pageJson = mockMvc.perform(get("/api/tasks").param("search", marker)
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(pageJson);
        JsonNode content = root.get("content");
        assertTrue(content.isArray() && content.size() >= 1);
        boolean hit = false;
        for (JsonNode row : content) {
            if (marker.equals(row.get("title").asText())) {
                hit = true;
                break;
            }
        }
        assertTrue(hit);
    }

    @Test
    void dailyTaskEndpoint_authorized() throws Exception {
        String u = TestId.uniq("daily");
        authService.register(new AuthRequest(u, "p", u + "@z.dev", "STUDENT"));
        String token = authService.login(new AuthRequest(u, "p", null, null)).token();
        var res = mockMvc.perform(get("/api/daily-task")
                        .header("Authorization", "Bearer " + token));
        int st = res.andReturn().getResponse().getStatus();
        assertTrue(st == 200 || st == 204);
    }

    @Test
    void challenge_joinAndList_secondJoin400() throws Exception {
        String teacher = TestId.uniq("ch_t");
        String student = TestId.uniq("ch_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));
        long tid = userRepo.findByUsername(teacher).orElseThrow().getId();
        long sid = userRepo.findByUsername(student).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        LocalDateTime start = LocalDateTime.now().plusHours(3);
        LocalDateTime end = start.plusDays(2);
        String chBody = objectMapper.writeValueAsString(
                new ChallengeCreateRequest("Cup " + TestId.uniq("c"), "desc", start, end, 30));

        String chResp = mockMvc.perform(post("/api/admin/challenges")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long challengeId = objectMapper.readTree(chResp).get("id").asLong();

        mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "Arena task", null, "EASY", 10, null, "42", null, null, challengeId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/challenges")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isBadRequest());
    }

    @Test
    void articles_listAndGetById() throws Exception {
        String teacher = TestId.uniq("art_api");
        authService.register(new AuthRequest(teacher, "p", teacher + "@z.dev", "TEACHER"));
        String token = authService.login(new AuthRequest(teacher, "p", null, null)).token();

        String title = "Readme " + TestId.uniq("r");
        String createRes = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ArticleCreateRequest(title, "body", "Теория", 1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createRes).get("id").asLong();

        mockMvc.perform(get("/api/articles").param("q", title)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void aiHint_postReturnsHint() throws Exception {
        String teacher = TestId.uniq("hint_t");
        authService.register(new AuthRequest(teacher, "p", teacher + "@z.dev", "TEACHER"));
        String token = authService.login(new AuthRequest(teacher, "p", null, null)).token();

        String taskRes = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "Hinted", "Выведите число 1", "EASY", 1, null, "1", "use printf", null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskRes).get("id").asLong();

        String hintRes = mockMvc.perform(post("/api/ai/hint")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HintRequest(taskId, "int main(){return 0;}", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(objectMapper.readTree(hintRes).get("hint").asText().isBlank());
    }

    @Test
    void adminStudents_listReturns200() throws Exception {
        String teacher = TestId.uniq("grp_t");
        authService.register(new AuthRequest(teacher, "p", teacher + "@z.dev", "TEACHER"));
        String token = authService.login(new AuthRequest(teacher, "p", null, null)).token();

        mockMvc.perform(get("/api/admin/students")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void submissionHistory_afterSubmit() throws Exception {
        String teacher = TestId.uniq("hist_t");
        String student = TestId.uniq("hist_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));
        long tid = userRepo.findByUsername(teacher).orElseThrow().getId();
        long sid = userRepo.findByUsername(student).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sid, tid));

        sandbox.setHandler((c, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "99\n", "", ExecutionBackend.LOCAL_GCC));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        String taskRes = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "Hist", null, "EASY", 5, null, "99", null, null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskRes).get("id").asLong();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + sTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmissionRequest(taskId, "#include <stdio.h>\nint main(){return 0;}\n"))))
                .andExpect(status().isOk());

        String histJson = mockMvc.perform(get("/api/submissions/task/" + taskId)
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(objectMapper.readTree(histJson).isArray()
                && objectMapper.readTree(histJson).size() >= 1);
    }

    @Test
    void detachTaskFromLesson() throws Exception {
        String teacher = TestId.uniq("det_t");
        authService.register(new AuthRequest(teacher, "p", teacher + "@z.dev", "TEACHER"));
        String token = authService.login(new AuthRequest(teacher, "p", null, null)).token();

        long taskId = objectMapper.readTree(mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "Detach me", null, "EASY", 1, null, "z", null, null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        int ord = 500 + Math.floorMod(teacher.hashCode(), 200);
        long lessonId = objectMapper.readTree(mockMvc.perform(post("/api/admin/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LessonCreateRequest("L", "", "", ord, List.of(taskId)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/admin/lessons/" + lessonId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}

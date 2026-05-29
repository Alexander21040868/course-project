package org.example.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.AuthRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSandboxConfig.class)
class ApiEndToEndIntegrationTest {

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
    void stubSandbox() {
        sandbox.setHandler((code, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "9\n", "", ExecutionBackend.LOCAL_GCC));
    }

    @Test
    void authLoginAndProfile() throws Exception {
        String u = TestId.uniq("http_user");
        authService.register(new AuthRequest(u, "secret", u + "@z.dev", "STUDENT"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest(u, "secret", null, null))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCreatesTask_studentSubmitsViaApi() throws Exception {
        String teacher = TestId.uniq("http_t");
        String student = TestId.uniq("http_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));

        long tId = userRepo.findByUsername(teacher).orElseThrow().getId();
        long sId = userRepo.findByUsername(student).orElseThrow().getId();
        studentTeacherRepo.save(new StudentTeacherLink(sId, tId));

        String teacherToken = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String studentToken = authService.login(new AuthRequest(student, "p2", null, null)).token();

        var taskJson = objectMapper.writeValueAsString(new TaskCreateRequest(
                "HTTP task", null, "EASY", 10,
                null, "9", null, null, null));

        MvcResult taskRes = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andReturn();

        long taskId = objectMapper.readTree(taskRes.getResponse().getContentAsString()).get("id").asLong();

        int order = 100 + (int) (taskId % 500);
        mockMvc.perform(post("/api/admin/lessons")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LessonCreateRequest("API dungeon", "", "", order, List.of(taskId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lessons").param("teacherUsername", teacher)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        var subJson = objectMapper.writeValueAsString(
                new SubmissionRequest(taskId, "#include <stdio.h>\nint main(){return 0;}\n"));

        MvcResult sub = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subJson))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("CORRECT",
                objectMapper.readTree(sub.getResponse().getContentAsString()).get("status").asText());
    }

    @Test
    void studentCannotAccessAdmin() throws Exception {
        String u = TestId.uniq("stuonly");
        authService.register(new AuthRequest(u, "p", u + "@z.dev", "STUDENT"));
        String token = authService.login(new AuthRequest(u, "p", null, null)).token();

        mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}

package org.example.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.AuthRequest;
import org.example.dto.HintRequest;
import org.example.dto.SubmissionRequest;
import org.example.dto.TaskCreateRequest;
import org.example.entity.StudentTeacherLink;
import org.example.repository.StudentTeacherRepository;
import org.example.repository.TaskRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSandboxConfig.class)
class ExecutionAndSandboxApiIntegrationTest {

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
    TaskRepository taskRepo;
    @Autowired
    ControllableSandboxDockerService sandbox;

    @BeforeEach
    void resetSandbox() {
        sandbox.setHandler((c, in) ->
                new CExecutionResult(CExecutionResult.Kind.OK, "1\n", "", ExecutionBackend.LOCAL_GCC));
    }

    @Test
    void submit_unknownTask_returnsBadRequest() throws Exception {
        String u = TestId.uniq("sub_uk");
        authService.register(new AuthRequest(u, "p", u + "@z.dev", "STUDENT"));
        String tok = authService.login(new AuthRequest(u, "p", null, null)).token();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmissionRequest(-99999L, "#include <stdio.h>\nint main(){return 0;}\n"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_codeWithoutMain_returnsWrong() throws Exception {
        String teacher = TestId.uniq("ex_t");
        String student = TestId.uniq("ex_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(student).orElseThrow().getId(),
                userRepo.findByUsername(teacher).orElseThrow().getId()));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        String taskJson = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "NoMain", null, "EASY", 1, null, "1", null, null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + sTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmissionRequest(taskId, "int x = 1;\n"))))
                .andExpect(status().isOk());
    }

    @Test
    void aiHint_postReturnsJson() throws Exception {
        String teacher = TestId.uniq("hint_api_t");
        String student = TestId.uniq("hint_api_s");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(student, "p2", student + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(student).orElseThrow().getId(),
                userRepo.findByUsername(teacher).orElseThrow().getId()));

        String tTok = authService.login(new AuthRequest(teacher, "p1", null, null)).token();
        String sTok = authService.login(new AuthRequest(student, "p2", null, null)).token();

        String taskJson = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "HintApi", "printf", "EASY", 1, null, "x", null, null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        mockMvc.perform(post("/api/ai/hint")
                        .header("Authorization", "Bearer " + sTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HintRequest(taskId, "int main(){return 0;}", ""))))
                .andExpect(status().isOk());
    }

    @Test
    void taskHistory_forbiddenForForeignStudent_returnsBadRequest() throws Exception {
        String t1 = TestId.uniq("ht1");
        String t2 = TestId.uniq("ht2");
        String s1 = TestId.uniq("hs1");
        authService.register(new AuthRequest(t1, "p", t1 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(t2, "p", t2 + "@z.dev", "TEACHER"));
        authService.register(new AuthRequest(s1, "p", s1 + "@z.dev", "STUDENT"));
        studentTeacherRepo.save(new StudentTeacherLink(
                userRepo.findByUsername(s1).orElseThrow().getId(),
                userRepo.findByUsername(t1).orElseThrow().getId()));

        String tok2 = authService.login(new AuthRequest(t2, "p", null, null)).token();
        String sTok = authService.login(new AuthRequest(s1, "p", null, null)).token();

        String res = mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + tok2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest(
                                "Foreign", null, "EASY", 1, null, "1", null, null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(res).get("id").asLong();
        var task = taskRepo.findById(taskId).orElseThrow();
        task.setCatalogVisibleFrom(LocalDateTime.now().plusDays(30));
        taskRepo.save(task);

        mockMvc.perform(get("/api/submissions/task/" + taskId)
                        .header("Authorization", "Bearer " + sTok))
                .andExpect(status().isBadRequest());
    }
}

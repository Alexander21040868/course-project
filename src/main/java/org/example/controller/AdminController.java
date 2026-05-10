package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.*;
import org.example.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LessonService lessonService;
    private final TaskService taskService;
    private final ChallengeService challengeService;
    private final StudentProgressService studentProgressService;
    private final ArticleService articleService;
    private final StudyGroupService studyGroupService;
    private final GroupInviteService inviteService;

    public AdminController(LessonService lessonService, TaskService taskService,
                           ChallengeService challengeService, StudentProgressService studentProgressService,
                           ArticleService articleService, StudyGroupService studyGroupService,
                           GroupInviteService inviteService) {
        this.lessonService = lessonService;
        this.taskService = taskService;
        this.challengeService = challengeService;
        this.studentProgressService = studentProgressService;
        this.articleService = articleService;
        this.studyGroupService = studyGroupService;
        this.inviteService = inviteService;
    }

    @PostMapping("/lessons")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody LessonCreateRequest req,
                                                   Principal principal) {
        return ResponseEntity.ok(lessonService.create(req, principal.getName()));
    }

    @PutMapping("/lessons/by-order/{currentOrder}")
    public ResponseEntity<LessonDto> updateLessonByOrder(@PathVariable("currentOrder") int currentOrder,
                                                         @Valid @RequestBody LessonCreateRequest req,
                                                         Principal principal) {
        return ResponseEntity.ok(lessonService.updateByOrderForTeacher(currentOrder, req, principal.getName()));
    }

    @DeleteMapping("/lessons/by-order/{orderIndex}")
    public ResponseEntity<Void> deleteLessonByOrder(@PathVariable("orderIndex") int orderIndex,
                                                    Principal principal) {
        lessonService.deleteByOrderForTeacher(orderIndex, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lessons/{id}")
    public ResponseEntity<LessonDto> updateLesson(@PathVariable("id") Long id,
                                                   @Valid @RequestBody LessonCreateRequest req) {
        return ResponseEntity.ok(lessonService.update(id, req));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable("id") Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lessons/{id}/tasks")
    public ResponseEntity<Map<String, Object>> attachTasksToLesson(@PathVariable("id") Long id,
                                                                    @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("taskIds", List.of());
        List<Long> attached = lessonService.attachTasks(id, ids);
        return ResponseEntity.ok(Map.of("attachedTaskIds", attached));
    }

    @DeleteMapping("/lessons/{id}/tasks/{taskId}")
    public ResponseEntity<Void> detachTaskFromLesson(@PathVariable("id") Long id,
                                                     @PathVariable("taskId") Long taskId,
                                                     Principal principal) {
        lessonService.detachTask(id, taskId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons/by-order/{orderIndex}")
    public ResponseEntity<LessonDto> getMyLessonByOrder(@PathVariable("orderIndex") int orderIndex,
                                                        Principal principal) {
        return ResponseEntity.ok(lessonService.findByOrderForTeacher(orderIndex, principal.getName()));
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskCreateRequest req, Principal principal) {
        return ResponseEntity.ok(taskService.create(req, principal.getName()));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskEditDto> getTaskForEdit(@PathVariable("id") Long id) {
        return ResponseEntity.ok(taskService.findForEdit(id));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable("id") Long id,
                                               @Valid @RequestBody TaskCreateRequest req) {
        return ResponseEntity.ok(taskService.update(id, req));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/challenges")
    public ResponseEntity<ChallengeDto> createChallenge(@Valid @RequestBody ChallengeCreateRequest req,
                                                         Principal principal) {
        return ResponseEntity.ok(challengeService.create(req, principal.getName()));
    }

    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeDto>> listChallenges(Principal principal) {
        return ResponseEntity.ok(challengeService.findAll(principal.getName()));
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentProgressDto>> getStudentsProgress(Principal principal) {
        return ResponseEntity.ok(studentProgressService.listGroup(principal.getName()));
    }

    @GetMapping("/students/available")
    public ResponseEntity<List<StudentSummaryDto>> getAvailableStudents(
            @RequestParam(value = "search", required = false) String search,
            Principal principal) {
        return ResponseEntity.ok(studentProgressService.listAvailable(principal.getName(), search));
    }

    @PostMapping("/students/{id}/invite")
    public ResponseEntity<Void> inviteStudent(@PathVariable("id") Long id,
                                              @RequestBody(required = false) Map<String, Object> body,
                                              Principal principal) {
        Long groupId = null;
        if (body != null && body.get("groupId") != null) {
            Object g = body.get("groupId");
            if (g instanceof Number n) groupId = n.longValue();
        }
        inviteService.invite(id, groupId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/students/{id}/assign")
    public ResponseEntity<Void> removeStudentFromGroup(@PathVariable("id") Long id, Principal principal) {
        studentProgressService.removeFromGroup(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{id}/group")
    public ResponseEntity<Void> moveStudentToSubgroup(@PathVariable("id") Long id,
                                                      @RequestBody Map<String, Object> body,
                                                      Principal principal) {
        Long groupId = null;
        Object g = body.get("groupId");
        if (g instanceof Number n) groupId = n.longValue();
        studyGroupService.moveStudent(id, groupId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(studentProgressService.getDetail(id, principal.getName()));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<StudyGroupDto>> listGroups(Principal principal) {
        return ResponseEntity.ok(studyGroupService.listForTeacher(principal.getName()));
    }

    @PostMapping("/groups")
    public ResponseEntity<StudyGroupDto> createGroup(@RequestBody Map<String, String> body,
                                                      Principal principal) {
        return ResponseEntity.ok(studyGroupService.create(principal.getName(), body.get("name")));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("id") Long id, Principal principal) {
        studyGroupService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/articles")
    public ResponseEntity<ArticleDto> createArticle(@Valid @RequestBody ArticleCreateRequest req,
                                                     Principal principal) {
        return ResponseEntity.ok(articleService.create(req, principal.getName()));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ArticleDto> updateArticle(@PathVariable("id") Long id,
                                                    @Valid @RequestBody ArticleCreateRequest req) {
        return ResponseEntity.ok(articleService.update(id, req));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("id") Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

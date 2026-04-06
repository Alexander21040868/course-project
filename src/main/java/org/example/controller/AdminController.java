package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.*;
import org.example.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminController {

    private final LessonService lessonService;
    private final TaskService taskService;
    private final ChallengeService challengeService;
    private final StudentProgressService studentProgressService;
    private final ArticleService articleService;
    private final PdfExportService pdfExportService;

    public AdminController(LessonService lessonService, TaskService taskService,
                           ChallengeService challengeService, StudentProgressService studentProgressService,
                           ArticleService articleService, PdfExportService pdfExportService) {
        this.lessonService = lessonService;
        this.taskService = taskService;
        this.challengeService = challengeService;
        this.studentProgressService = studentProgressService;
        this.articleService = articleService;
        this.pdfExportService = pdfExportService;
    }

    @PostMapping("/lessons")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody LessonCreateRequest req,
                                                   Principal principal) {
        return ResponseEntity.ok(lessonService.create(req, principal.getName()));
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

    @PostMapping("/tasks")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskCreateRequest req) {
        return ResponseEntity.ok(taskService.create(req));
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

    @GetMapping("/students")
    public ResponseEntity<List<StudentProgressDto>> getStudentsProgress() {
        return ResponseEntity.ok(studentProgressService.listStudents());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(studentProgressService.getDetail(id));
    }

    @GetMapping(value = "/students/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStudentPdf(@PathVariable("id") Long id) {
        byte[] pdf = pdfExportService.exportStudentReport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"codequest-progress.pdf\"")
                .body(pdf);
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

package org.example.controller;

import org.example.dto.LessonDto;
import org.example.service.LessonService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public ResponseEntity<Page<LessonDto>> getAll(
            @RequestParam(value = "teacherUsername", required = false) String teacherUsername,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Principal principal) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(lessonService.findPageFor(principal.getName(), teacherUsername, page, safeSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDto> getById(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(lessonService.findById(id, principal.getName()));
    }
}

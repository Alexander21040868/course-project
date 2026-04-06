package org.example.controller;

import org.example.dto.LessonDto;
import org.example.service.LessonService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public ResponseEntity<Page<LessonDto>> getAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(lessonService.findPage(page, Math.min(Math.max(size, 1), 100)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(lessonService.findById(id));
    }
}

package org.example.controller;

import org.example.dto.ArticleDto;
import org.example.dto.ArticleSummaryDto;
import org.example.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ResponseEntity<List<ArticleSummaryDto>> list(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(articleService.search(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.findById(id));
    }
}

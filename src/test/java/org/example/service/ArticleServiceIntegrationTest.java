package org.example.service;

import org.example.dto.ArticleCreateRequest;
import org.example.dto.AuthRequest;
import org.example.support.TestId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ArticleServiceIntegrationTest {

    @Autowired
    AuthService authService;
    @Autowired
    ArticleService articleService;

    @Test
    void teacherCreatesArticle_searchFindsIt() {
        String teacher = TestId.uniq("artt");
        authService.register(new AuthRequest(teacher, "p1", teacher + "@a.dev", "TEACHER"));

        var created = articleService.create(
                new ArticleCreateRequest("Lambda в C (шаг 1)", "Текст статьи", "Теория", 1),
                teacher);

        var hits = articleService.search("Lambda");
        assertTrue(hits.stream().anyMatch(a -> a.id().equals(created.id())));

        ArticleCreateRequest upd = new ArticleCreateRequest("Lambda в C (шаг 2)", "Новый текст", "Теория", 2);
        var updated = articleService.update(created.id(), upd, teacher);
        assertEquals("Lambda в C (шаг 2)", updated.title());
    }
}

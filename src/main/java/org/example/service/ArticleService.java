package org.example.service;

import org.example.dto.ArticleCreateRequest;
import org.example.dto.ArticleDto;
import org.example.dto.ArticleSummaryDto;
import org.example.entity.Article;
import org.example.entity.User;
import org.example.repository.ArticleRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepo;
    private final UserRepository userRepo;

    public ArticleService(ArticleRepository articleRepo, UserRepository userRepo) {
        this.articleRepo = articleRepo;
        this.userRepo = userRepo;
    }

    public List<ArticleSummaryDto> search(String q) {
        if (q == null || q.isBlank())
            return articleRepo.findAllByOrderByOrderIndexAsc().stream()
                    .map(a -> new ArticleSummaryDto(a.getId(), a.getTitle(), a.getCategory(), a.getOrderIndex()))
                    .toList();
        return articleRepo.findByTitleContainingIgnoreCaseOrderByOrderIndexAsc(q.trim()).stream()
                .map(a -> new ArticleSummaryDto(a.getId(), a.getTitle(), a.getCategory(), a.getOrderIndex()))
                .toList();
    }

    public ArticleDto findById(Long id) {
        return articleRepo.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
    }

    @Transactional
    public ArticleDto create(ArticleCreateRequest req, String authorUsername) {
        User author = userRepo.findByUsername(authorUsername).orElse(null);
        Article a = new Article();
        a.setTitle(req.title().trim());
        a.setContent(req.content());
        a.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim() : "Справочник");
        a.setOrderIndex(req.orderIndex());
        a.setAuthor(author);
        return toDto(articleRepo.save(a));
    }

    @Transactional
    public ArticleDto update(Long id, ArticleCreateRequest req) {
        Article a = articleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        a.setTitle(req.title().trim());
        a.setContent(req.content());
        a.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim() : "Справочник");
        a.setOrderIndex(req.orderIndex());
        return toDto(articleRepo.save(a));
    }

    @Transactional
    public void delete(Long id) {
        articleRepo.deleteById(id);
    }

    private ArticleDto toDto(Article a) {
        String author = a.getAuthor() != null ? a.getAuthor().getUsername() : null;
        return new ArticleDto(a.getId(), a.getTitle(), a.getContent(), a.getCategory(), a.getOrderIndex(), author);
    }
}

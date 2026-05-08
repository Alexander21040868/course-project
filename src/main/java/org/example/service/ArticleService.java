package org.example.service;

import org.example.dto.ArticleCreateRequest;
import org.example.dto.ArticleDto;
import org.example.dto.ArticleSummaryDto;
import org.example.entity.Article;
import org.example.entity.User;
import org.example.repository.ArticleRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepo;
    private final UserRepository userRepo;

    public ArticleService(ArticleRepository articleRepo, UserRepository userRepo) {
        this.articleRepo = articleRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<ArticleSummaryDto> search(String q) {
        if (q == null || q.isBlank()) {
            return articleRepo.findAllByOrderByOrderIndexAsc().stream()
                    .map(a -> new ArticleSummaryDto(a.getId(), a.getTitle(), a.getCategory(), a.getOrderIndex()))
                    .toList();
        }
        String t = q.trim();
        Optional<Long> byId = parseNumericIdQuery(t);
        if (byId.isPresent()) {
            return articleRepo.findById(byId.get())
                    .map(a -> List.of(new ArticleSummaryDto(a.getId(), a.getTitle(), a.getCategory(), a.getOrderIndex())))
                    .orElseGet(List::of);
        }
        return articleRepo.findByTitleContainingIgnoreCaseOrderByOrderIndexAsc(t).stream()
                .map(a -> new ArticleSummaryDto(a.getId(), a.getTitle(), a.getCategory(), a.getOrderIndex()))
                .toList();
    }

    private static Optional<Long> parseNumericIdQuery(String trimmed) {
        if (!trimmed.matches("#?\\d+")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(trimmed.replaceFirst("^#", "")));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public ArticleDto findById(Long id) {
        return articleRepo.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
    }

    @Transactional(readOnly = false)
    public ArticleDto create(ArticleCreateRequest req, String authorUsername) {
        User author = userRepo.findByUsername(authorUsername).orElse(null);
        Article a = new Article();
        a.setTitle(req.title().trim());
        a.setContent(req.content());
        a.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim() : "Справочник");
        a.setOrderIndex(req.orderIndex());
        a.setAuthor(author);
        Article saved = articleRepo.save(a);
        log.info("Статья создана: id={} «{}»", saved.getId(), saved.getTitle());
        return toDto(saved);
    }

    @Transactional(readOnly = false)
    public ArticleDto update(Long id, ArticleCreateRequest req) {
        Article a = articleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        a.setTitle(req.title().trim());
        a.setContent(req.content());
        a.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim() : "Справочник");
        a.setOrderIndex(req.orderIndex());
        log.info("Статья обновлена: id={}", id);
        return toDto(articleRepo.save(a));
    }

    @Transactional(readOnly = false)
    public void delete(Long id) {
        log.info("Статья удалена: id={}", id);
        articleRepo.deleteById(id);
    }

    private ArticleDto toDto(Article a) {
        String author = a.getAuthor() != null ? a.getAuthor().getUsername() : null;
        return new ArticleDto(a.getId(), a.getTitle(), a.getContent(), a.getCategory(), a.getOrderIndex(), author);
    }
}

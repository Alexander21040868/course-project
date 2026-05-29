package org.example.repository;

import org.example.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByTitleContainingIgnoreCaseOrderByOrderIndexAsc(String q);
    List<Article> findAllByOrderByOrderIndexAsc();
}

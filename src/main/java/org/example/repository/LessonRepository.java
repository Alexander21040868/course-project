package org.example.repository;

import org.example.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findAllByOrderByOrderIndexAsc();

    Page<Lesson> findAllByOrderByOrderIndexAsc(Pageable pageable);

    Page<Lesson> findByAuthorIdOrderByOrderIndexAsc(Long authorId, Pageable pageable);

    List<Lesson> findByAuthorIdOrderByOrderIndexAsc(Long authorId);

    Optional<Lesson> findByAuthorIdAndOrderIndex(Long authorId, int orderIndex);
}

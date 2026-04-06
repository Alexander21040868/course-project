package org.example.repository;

import org.example.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findAllByOrderByOrderIndexAsc();

    Page<Lesson> findAllByOrderByOrderIndexAsc(Pageable pageable);
}

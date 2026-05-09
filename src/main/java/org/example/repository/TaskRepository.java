package org.example.repository;

import org.example.entity.Difficulty;
import org.example.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTitleContainingIgnoreCaseOrderByIdAsc(String title);
    Page<Task> findByTitleContainingIgnoreCaseOrderByIdAsc(String title, Pageable pageable);
    List<Task> findAllByOrderByIdAsc();
    Page<Task> findAllByOrderByIdAsc(Pageable pageable);
    long countByDifficulty(Difficulty difficulty);
}

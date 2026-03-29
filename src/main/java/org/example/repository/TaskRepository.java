package org.example.repository;

import org.example.entity.Difficulty;
import org.example.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByLessonIdOrderByOrderIndexAsc(Long lessonId);
    long countByDifficulty(Difficulty difficulty);
}

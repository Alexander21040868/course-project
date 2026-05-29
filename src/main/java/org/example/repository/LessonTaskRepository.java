package org.example.repository;

import org.example.entity.LessonTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonTaskRepository extends JpaRepository<LessonTask, LessonTask.LessonTaskId> {

    List<LessonTask> findByLessonIdOrderByOrderIndexAsc(Long lessonId);

    long countByLessonId(Long lessonId);

    Optional<LessonTask> findByLessonIdAndTaskId(Long lessonId, Long taskId);

    @Modifying
    @Query("delete from LessonTask lt where lt.lesson.id = :lessonId and lt.task.id = :taskId")
    void deleteLink(@Param("lessonId") Long lessonId, @Param("taskId") Long taskId);

    @Modifying
    @Query("delete from LessonTask lt where lt.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") Long lessonId);

    @Modifying
    @Query("delete from LessonTask lt where lt.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}

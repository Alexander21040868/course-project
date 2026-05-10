package org.example.repository;

import org.example.entity.Challenge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    @EntityGraph(attributePaths = "tasks")
    @Query("SELECT c FROM Challenge c WHERE c.endTime > :now ORDER BY c.startTime ASC")
    List<Challenge> findForArena(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = "tasks")
    @Query("SELECT c FROM Challenge c ORDER BY c.createdAt DESC")
    List<Challenge> findAllForAdmin();

    @EntityGraph(attributePaths = "tasks")
    @Query("SELECT c FROM Challenge c WHERE c.id = :id")
    Optional<Challenge> findDetailById(@Param("id") Long id);

    @Query("SELECT c.id FROM Challenge c WHERE c.rated = false AND c.endTime < :now")
    List<Long> findIdsUnratedEnded(@Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Challenge c JOIN c.tasks t WHERE t.id = :taskId")
    boolean existsWithTaskId(@Param("taskId") Long taskId);

    @Query("SELECT c.createdBy.id FROM Challenge c JOIN c.tasks t WHERE t.id = :taskId")
    Optional<Long> findOrganizerUserIdByTaskId(@Param("taskId") Long taskId);

    @EntityGraph(attributePaths = "tasks")
    @Query("SELECT DISTINCT c FROM Challenge c JOIN c.tasks t WHERE t.id = :taskId AND c.startTime <= :now AND c.endTime >= :now")
    Optional<Challenge> findActiveCoveringTask(@Param("taskId") Long taskId, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Challenge c JOIN c.tasks t WHERE t.id = :taskId")
    Optional<Challenge> findFirstByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT c FROM Challenge c WHERE c.startTime <= :now AND SIZE(c.tasks) = 0")
    List<Challenge> findStartedWithNoTasks(@Param("now") LocalDateTime now);
}

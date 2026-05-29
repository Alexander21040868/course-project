package org.example.repository;

import org.example.entity.Difficulty;
import org.example.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTitleContainingIgnoreCaseOrderByIdAsc(String title);
    Page<Task> findByTitleContainingIgnoreCaseOrderByIdAsc(String title, Pageable pageable);
    List<Task> findAllByOrderByIdAsc();
    Page<Task> findAllByOrderByIdAsc(Pageable pageable);
    long countByDifficulty(Difficulty difficulty);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.difficulty = :d
            AND (t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now)""")
    long countReleasedByDifficulty(@Param("d") Difficulty difficulty, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now""")
    long countReleasedAt(@Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Task t
            WHERE t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now
            ORDER BY t.id ASC""")
    List<Task> findReleasedOrderByIdAsc(@Param("now") LocalDateTime now);

    @Query("""
            SELECT t FROM Task t
            WHERE (t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now
            OR (:now < t.catalogVisibleFrom AND (
                 (NOT EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id) AND t.author.id = :vid)
                 OR EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id AND ch.createdBy.id = :vid)
            )))
            ORDER BY t.id ASC""")
    List<Task> findCatalogOrderByIdAsc(@Param("now") LocalDateTime now, @Param("vid") long viewerId);

    @Query("""
            SELECT t FROM Task t
            WHERE (t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now
            OR (:now < t.catalogVisibleFrom AND (
                 (NOT EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id) AND t.author.id = :vid)
                 OR EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id AND ch.createdBy.id = :vid)
            )))
            ORDER BY t.id ASC""")
    Page<Task> pageCatalog(@Param("now") LocalDateTime now, @Param("vid") long viewerId, Pageable pageable);

    @Query("""
            SELECT t FROM Task t
            WHERE (t.catalogVisibleFrom IS NULL OR t.catalogVisibleFrom <= :now
            OR (:now < t.catalogVisibleFrom AND (
                 (NOT EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id) AND t.author.id = :vid)
                 OR EXISTS (SELECT 1 FROM Challenge ch JOIN ch.tasks tk WHERE tk.id = t.id AND ch.createdBy.id = :vid)
            )))
            AND LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.id ASC""")
    Page<Task> searchCatalog(@Param("q") String q, @Param("now") LocalDateTime now, @Param("vid") long viewerId,
                             Pageable pageable);
}

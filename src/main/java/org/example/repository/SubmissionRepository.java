package org.example.repository;

import org.example.entity.Submission;
import org.example.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdAndTaskIdOrderBySubmittedAtDesc(Long userId, Long taskId);
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    long countByUserIdAndStatus(Long userId, SubmissionStatus status);
    boolean existsByUserIdAndTaskIdAndStatus(Long userId, Long taskId, SubmissionStatus status);

    @Query("SELECT COUNT(DISTINCT s.task.id) FROM Submission s WHERE s.user.id = :userId AND s.status = :status")
    long countDistinctTaskByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubmissionStatus status);
}

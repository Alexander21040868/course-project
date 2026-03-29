package org.example.repository;

import org.example.entity.Submission;
import org.example.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdAndTaskIdOrderBySubmittedAtDesc(Long userId, Long taskId);
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    long countByUserIdAndStatus(Long userId, SubmissionStatus status);
    boolean existsByUserIdAndTaskIdAndStatus(Long userId, Long taskId, SubmissionStatus status);
    long countDistinctTaskByUserIdAndStatus(Long userId, SubmissionStatus status);
}

package org.example.repository;

import org.example.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByTaskIdOrderByOrderIndexAsc(Long taskId);
    List<TestCase> findByTaskIdAndSampleTrueOrderByOrderIndexAsc(Long taskId);
    void deleteByTaskId(Long taskId);
}

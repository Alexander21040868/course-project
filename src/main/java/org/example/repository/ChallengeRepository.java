package org.example.repository;

import org.example.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByEndTimeAfterOrderByStartTimeAsc(LocalDateTime now);
    List<Challenge> findAllByOrderByCreatedAtDesc();
}

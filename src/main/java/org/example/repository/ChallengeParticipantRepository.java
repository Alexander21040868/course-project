package org.example.repository;

import org.example.entity.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {
    List<ChallengeParticipant> findByChallengeIdOrderByTasksSolvedDesc(Long challengeId);
    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
}

package org.example.scheduler;

import org.example.service.ChallengeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChallengePurgeScheduler {

    private final ChallengeService challengeService;

    public ChallengePurgeScheduler(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Scheduled(fixedRate = 60_000)
    public void tick() {
        challengeService.deleteStartedChallengesWithNoTasks();
        challengeService.finalizeEndedUnratedChallenges();
    }
}

package org.example.controller;

import org.example.dto.LeaderboardEntryDto;
import org.example.entity.Role;
import org.example.entity.SubmissionStatus;
import org.example.repository.SubmissionRepository;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/leaderboard")
@Transactional(readOnly = true)
public class LeaderboardController {

    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;

    public LeaderboardController(UserRepository userRepo, SubmissionRepository submissionRepo) {
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> get() {
        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardEntryDto> board = userRepo.findTop50ByRoleOrderByXpDesc(Role.STUDENT)
                .stream()
                .map(u -> new LeaderboardEntryDto(
                        rank.getAndIncrement(), u.getUsername(), u.getXp(), u.getLevel(),
                        submissionRepo.countDistinctTaskByUserIdAndStatus(u.getId(), SubmissionStatus.CORRECT)
                ))
                .toList();
        return ResponseEntity.ok(board);
    }
}

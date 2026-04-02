package org.example.controller;

import org.example.dto.LeaderboardEntryDto;
import org.example.entity.Role;
import org.example.entity.SubmissionStatus;
import org.example.repository.SubmissionRepository;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<LeaderboardEntryDto>> get(@RequestParam(defaultValue = "rating") String sort) {
        var users = "xp".equals(sort)
                ? userRepo.findTop50ByRoleOrderByXpDesc(Role.STUDENT)
                : userRepo.findTop50ByRoleOrderByRatingDesc(Role.STUDENT);

        AtomicInteger rank = new AtomicInteger(1);
        var board = users.stream()
                .map(u -> new LeaderboardEntryDto(
                        rank.getAndIncrement(), u.getUsername(), u.getRating(), u.getXp(), u.getLevel(),
                        submissionRepo.countDistinctTaskByUserIdAndStatus(u.getId(), SubmissionStatus.CORRECT)
                ))
                .toList();
        return ResponseEntity.ok(board);
    }
}

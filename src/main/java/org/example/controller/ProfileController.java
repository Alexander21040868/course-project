package org.example.controller;

import org.example.dto.AchievementDto;
import org.example.dto.ProfileDto;
import org.example.entity.SubmissionStatus;
import org.example.entity.User;
import org.example.repository.SubmissionRepository;
import org.example.repository.UserRepository;
import org.example.service.GamificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;
    private final GamificationService gamificationService;

    public ProfileController(UserRepository userRepo, SubmissionRepository submissionRepo,
                             GamificationService gamificationService) {
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(Principal principal) {
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        long solvedCount = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);
        List<AchievementDto> achievements = gamificationService.getUserAchievements(user.getId());

        return ResponseEntity.ok(new ProfileDto(
                user.getUsername(), user.getRole().name(),
                user.getXp(), user.getLevel(),
                gamificationService.xpToNextLevel(user.getXp()),
                solvedCount, achievements
        ));
    }
}

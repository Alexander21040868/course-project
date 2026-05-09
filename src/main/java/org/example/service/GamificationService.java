package org.example.service;

import org.example.dto.AchievementDto;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GamificationService {

    private final UserRepository userRepo;
    private final AchievementRepository achievementRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final SubmissionRepository submissionRepo;
    private final TaskRepository taskRepo;
    private final LessonRepository lessonRepo;
    private final LessonTaskRepository lessonTaskRepo;
    private final NotificationService notificationService;

    public GamificationService(UserRepository userRepo,
                               AchievementRepository achievementRepo,
                               UserAchievementRepository userAchievementRepo,
                               SubmissionRepository submissionRepo,
                               TaskRepository taskRepo,
                               LessonRepository lessonRepo,
                               LessonTaskRepository lessonTaskRepo,
                               NotificationService notificationService) {
        this.userRepo = userRepo;
        this.achievementRepo = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.submissionRepo = submissionRepo;
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public int addXp(User user, int amount) {
        user.setXp(user.getXp() + amount);
        user.setLevel(calculateLevel(user.getXp()));
        userRepo.save(user);
        return amount;
    }

    @Transactional
    public List<AchievementDto> checkAndGrantAchievements(User user) {
        List<AchievementDto> earned = new ArrayList<>();
        long solved = submissionRepo.countDistinctTaskByUserIdAndStatus(user.getId(), SubmissionStatus.CORRECT);

        tryGrant(user, "FIRST_BLOOD", solved >= 1, earned);
        tryGrant(user, "SOLVER_10",   solved >= 10, earned);
        tryGrant(user, "SOLVER_50",   solved >= 50, earned);

        long totalEasy = taskRepo.countByDifficulty(Difficulty.EASY);
        long solvedEasy = submissionRepo.findByUserIdOrderBySubmittedAtDesc(user.getId()).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.CORRECT
                        && s.getTask().getDifficulty() == Difficulty.EASY)
                .map(s -> s.getTask().getId()).distinct().count();
        tryGrant(user, "ALL_EASY", totalEasy > 0 && solvedEasy >= totalEasy, earned);

        tryGrant(user, "LESSON_CLEAR", hasAnyLessonCleared(user.getId()), earned);

        LocalTime now = LocalTime.now();
        boolean isNight = now.getHour() >= 0 && now.getHour() < 5;
        if (isNight) tryGrant(user, "SPEEDRUN", true, earned);

        return earned;
    }

    private boolean hasAnyLessonCleared(Long userId) {
        for (Lesson lesson : lessonRepo.findAll()) {
            var links = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId());
            if (links.isEmpty()) continue;
            boolean allSolved = links.stream().allMatch(lt ->
                    submissionRepo.existsByUserIdAndTaskIdAndStatus(userId, lt.getTask().getId(), SubmissionStatus.CORRECT));
            if (allSolved) return true;
        }
        return false;
    }

    @Transactional
    public void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastSolvedDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            user.setStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            user.setStreak(user.getStreak() + 1);
        }

        if (user.getStreak() > user.getMaxStreak()) {
            user.setMaxStreak(user.getStreak());
        }
        user.setLastSolvedDate(today);
        userRepo.save(user);
    }

    public int calculateLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }

    public int xpToNextLevel(int xp) {
        int currentLevel = calculateLevel(xp);
        int nextLevelXp = (currentLevel) * (currentLevel) * 100;
        return nextLevelXp - xp;
    }

    public List<AchievementDto> getUserAchievements(Long userId) {
        return userAchievementRepo.findByUserId(userId).stream()
                .map(ua -> {
                    Achievement a = ua.getAchievement();
                    return new AchievementDto(a.getCode(), a.getName(), a.getDescription(), a.getIcon(), a.getXpReward());
                })
                .toList();
    }

    private void tryGrant(User user, String achievementCode, boolean condition, List<AchievementDto> result) {
        if (!condition) return;

        achievementRepo.findByCode(achievementCode).ifPresent(achievement -> {
            if (!userAchievementRepo.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
                UserAchievement ua = new UserAchievement();
                ua.setUser(user);
                ua.setAchievement(achievement);
                userAchievementRepo.save(ua);
                notificationService.notifyUser(user.getId(), "Новое достижение",
                        achievement.getName() + " — " + (achievement.getDescription() != null ? achievement.getDescription() : ""));

                addXp(user, achievement.getXpReward());
                result.add(new AchievementDto(
                        achievement.getCode(), achievement.getName(),
                        achievement.getDescription(), achievement.getIcon(),
                        achievement.getXpReward()
                ));
            }
        });
    }
}

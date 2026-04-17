package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);
    private static final int K = 40;

    private final ChallengeRepository challengeRepo;
    private final ChallengeParticipantRepository participantRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;
    private final NotificationService notificationService;

    public ChallengeService(ChallengeRepository challengeRepo,
                            ChallengeParticipantRepository participantRepo,
                            TaskRepository taskRepo, UserRepository userRepo,
                            SubmissionRepository submissionRepo,
                            NotificationService notificationService) {
        this.challengeRepo = challengeRepo;
        this.participantRepo = participantRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.notificationService = notificationService;
    }

    public List<ChallengeDto> findActive(String username) {
        Long userId = userRepo.findByUsername(username).map(User::getId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        return challengeRepo.findByEndTimeAfterOrderByStartTimeAsc(now).stream()
                .map(c -> toDto(c, userId, now)).toList();
    }

    public List<ChallengeDto> findAll(String username) {
        Long userId = userRepo.findByUsername(username).map(User::getId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        return challengeRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> toDto(c, userId, now)).toList();
    }

    @Transactional(readOnly = false)
    public ChallengeDto create(ChallengeCreateRequest req, String username) {
        User author = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Challenge ch = new Challenge();
        ch.setTitle(req.title());
        ch.setDescription(req.description());
        ch.setStartTime(req.startTime());
        ch.setEndTime(req.endTime());
        ch.setBonusXp(req.bonusXp() > 0 ? req.bonusXp() : 50);
        ch.setCreatedBy(author);
        ch.setTasks(taskRepo.findAllById(req.taskIds()));
        challengeRepo.save(ch);
        log.info("Челлендж создан: id={} «{}»", ch.getId(), ch.getTitle());
        notificationService.notifyAllStudents("Новый челлендж", ch.getTitle());

        return toDto(ch, author.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = false)
    public void join(Long challengeId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (participantRepo.existsByChallengeIdAndUserId(challengeId, user.getId()))
            throw new IllegalArgumentException("Вы уже участвуете");

        ChallengeParticipant cp = new ChallengeParticipant();
        cp.setChallenge(challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден")));
        cp.setUser(user);
        participantRepo.save(cp);
    }

    @Transactional
    public List<ChallengeResultDto> getResults(Long challengeId) {
        Challenge ch = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));

        List<Long> taskIds = ch.getTasks().stream().map(Task::getId).toList();
        List<ChallengeParticipant> participants =
                participantRepo.findByChallengeIdOrderByTasksSolvedDesc(challengeId);

        // id → {user, solved}
        List<long[]> standings = new ArrayList<>(); // [userId, rating, solved]
        Map<Long, User> userMap = new HashMap<>();

        for (ChallengeParticipant cp : participants) {
            User u = cp.getUser();
            int solved = 0;
            for (Long tid : taskIds)
                if (submissionRepo.existsByUserIdAndTaskIdAndStatus(u.getId(), tid, SubmissionStatus.CORRECT))
                    solved++;
            standings.add(new long[]{u.getId(), u.getRating(), solved});
            userMap.put(u.getId(), u);
        }
        standings.sort((a, b) -> Long.compare(b[2], a[2]));

        // ELO: считаем при завершении контеста (однократно)
        Map<Long, Integer> deltas = standings.size() >= 2
                ? calculateEloDeltas(standings) : Map.of();

        boolean ended = LocalDateTime.now().isAfter(ch.getEndTime());
        if (ended && !ch.isRated() && !deltas.isEmpty()) {
            for (var entry : standings) {
                User u = userMap.get(entry[0]);
                u.setRating(Math.max(0, u.getRating() + deltas.getOrDefault(u.getId(), 0)));
                userRepo.save(u);
            }
            ch.setRated(true);
            challengeRepo.save(ch);
        }

        AtomicInteger rank = new AtomicInteger(1);
        return standings.stream().map(s -> {
            User u = userMap.get(s[0]);
            return new ChallengeResultDto(rank.getAndIncrement(), u.getUsername(),
                    (int) s[2], u.getRating(), deltas.getOrDefault(u.getId(), 0));
        }).toList();
    }

    /**
     * Попарный ELO (как Codeforces):
     * Pij = 1 / (1 + 10^((Rj-Ri)/400))
     * Sij = 1 если solved_i > solved_j, 0.5 если ==, 0 если <
     * delta_i = K * Σ(Sij - Pij) / (N-1)
     */
    private Map<Long, Integer> calculateEloDeltas(List<long[]> standings) {
        Map<Long, Integer> deltas = new HashMap<>();
        int n = standings.size();

        for (long[] a : standings) {
            double delta = 0;
            for (long[] b : standings) {
                if (a[0] == b[0]) continue;
                double expected = 1.0 / (1 + Math.pow(10, (b[1] - a[1]) / 400.0));
                double actual = a[2] > b[2] ? 1.0 : a[2] == b[2] ? 0.5 : 0.0;
                delta += K * (actual - expected) / (n - 1);
            }
            deltas.put(a[0], (int) Math.round(delta));
        }
        return deltas;
    }

    private ChallengeDto toDto(Challenge c, Long userId, LocalDateTime now) {
        boolean joined = userId != null &&
                participantRepo.existsByChallengeIdAndUserId(c.getId(), userId);
        boolean active = now.isAfter(c.getStartTime()) && now.isBefore(c.getEndTime());
        return new ChallengeDto(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.getBonusXp(),
                c.getCreatedBy().getUsername(), c.getTasks().size(), joined, active);
    }
}

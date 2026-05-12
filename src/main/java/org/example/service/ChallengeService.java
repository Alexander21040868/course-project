package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.exception.ForbiddenOperationException;
import org.example.exception.NotFoundException;
import org.example.repository.*;
import org.example.util.XpLimits;
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
        return challengeRepo.findForArena(now).stream()
                .map(c -> toDto(c, userId, now)).toList();
    }

    public List<ChallengeDto> findAll(String username) {
        Long userId = userRepo.findByUsername(username).map(User::getId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        return challengeRepo.findAllForAdmin().stream()
                .map(c -> toDto(c, userId, now)).toList();
    }

    @Transactional(readOnly = false)
    public ChallengeDto create(ChallengeCreateRequest req, String username) {
        User author = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        LocalDateTime now = LocalDateTime.now();
        if (req.startTime().isBefore(now)) {
            throw new IllegalArgumentException("Время начала соревнования не может быть в прошлом");
        }
        if (!req.startTime().isBefore(req.endTime())) {
            throw new IllegalArgumentException("Время окончания должно быть позже старта");
        }

        Challenge ch = new Challenge();
        ch.setTitle(req.title());
        ch.setDescription(req.description());
        ch.setStartTime(req.startTime());
        ch.setEndTime(req.endTime());
        ch.setBonusXp(XpLimits.normalizeChallengeBonusXp(req.bonusXp()));
        ch.setCreatedBy(author);
        ch.setTasks(new ArrayList<>());
        challengeRepo.save(ch);
        log.info("Челлендж создан: id={} «{}»", ch.getId(), ch.getTitle());
        notificationService.notifyAllStudents("Новый челлендж", ch.getTitle());

        return toDto(ch, author.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = false)
    public void cancelBeforeStart(long challengeId, String username) {
        User actor = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Challenge ch = challengeRepo.findDetailById(challengeId)
                .orElseThrow(() -> new NotFoundException("Соревнование не найдено"));
        if (!ch.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Отменить может только организатор");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(ch.getStartTime())) {
            throw new IllegalArgumentException("Нельзя отменить соревнование после начала");
        }
        List<Long> notifyUserIds = participantRepo.findByChallengeIdOrderByTasksSolvedDesc(challengeId).stream()
                .map(cp -> cp.getUser().getId())
                .distinct()
                .toList();
        String title = ch.getTitle();
        challengeRepo.delete(ch);
        for (Long uid : notifyUserIds) {
            notificationService.notifyUser(uid, "Соревнование отменено",
                    "Организатор отменил соревнование «" + title + "».");
        }
    }

    @Transactional(readOnly = false)
    public void deleteStartedChallengesWithNoTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Challenge> doomed = challengeRepo.findStartedWithNoTasks(now);
        for (Challenge c : doomed) {
            log.info("Удалён пустой челлендж (наступил старт без задач): id={} «{}»", c.getId(), c.getTitle());
            challengeRepo.delete(c);
        }
    }

    @Transactional(readOnly = false)
    public void attachTask(long challengeId, long taskId, String username) {
        User actor = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Challenge ch = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));
        if (!ch.getCreatedBy().getId().equals(actor.getId())) {
            throw new IllegalArgumentException("Добавлять задачи в соревнование может только его создатель");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(ch.getStartTime())) {
            throw new IllegalArgumentException("Нельзя добавлять задачи после начала соревнования");
        }
        if (now.isAfter(ch.getEndTime())) {
            throw new IllegalArgumentException("Челлендж уже завершён");
        }
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        Optional<Challenge> existing = challengeRepo.findFirstByTaskId(taskId);
        if (existing.isPresent()) {
            if (existing.get().getId().equals(challengeId)) {
                return;
            }
            throw new IllegalArgumentException("Задача «" + task.getTitle() + "» уже в другом челлендже");
        }
        ensureTaskEligibleForChallenge(task, LocalDateTime.now());
        task.setCatalogVisibleFrom(ch.getStartTime());
        taskRepo.save(task);
        ch.getTasks().add(task);
        challengeRepo.save(ch);
    }

    public Long linkedChallengeId(long taskId) {
        return challengeRepo.findFirstByTaskId(taskId).map(Challenge::getId).orElse(null);
    }

    private void ensureTaskEligibleForChallenge(Task t, LocalDateTime now) {
        if (challengeRepo.existsWithTaskId(t.getId())) {
            throw new IllegalArgumentException("Задача «" + t.getTitle() + "» уже привязана к челленджу");
        }
        LocalDateTime cvf = t.getCatalogVisibleFrom();
        if (cvf == null) {
            if (submissionRepo.existsByTaskIdAndStatus(t.getId(), SubmissionStatus.CORRECT)) {
                throw new IllegalArgumentException(
                        "Задачу «" + t.getTitle() + "» уже решали — её нельзя закрыть под соревнование");
            }
        } else if (!now.isBefore(cvf)) {
            throw new IllegalArgumentException(
                    "Задача «" + t.getTitle() + "» уже в открытом каталоге");
        }
    }

    public int challengeBonusShareForFirstSolve(Task task, User user) {
        LocalDateTime now = LocalDateTime.now();
        Challenge ch = challengeRepo.findActiveCoveringTask(task.getId(), now).orElse(null);
        if (ch == null) {
            return 0;
        }
        if (!participantRepo.existsByChallengeIdAndUserId(ch.getId(), user.getId())) {
            return 0;
        }
        List<Long> sortedIds = ch.getTasks().stream().map(Task::getId).sorted().toList();
        int n = sortedIds.size();
        if (n == 0) {
            return 0;
        }
        int pos = sortedIds.indexOf(task.getId());
        if (pos < 0) {
            return 0;
        }
        int base = ch.getBonusXp() / n;
        int rem = ch.getBonusXp() % n;
        return base + (pos < rem ? 1 : 0);
    }

    @Transactional(readOnly = false)
    public void join(Long challengeId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Challenge ch = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(ch.getEndTime())) {
            throw new IllegalArgumentException("Челлендж уже завершён");
        }
        if (!now.isBefore(ch.getStartTime())) {
            throw new IllegalArgumentException("Регистрация закрыта: челлендж уже начался");
        }
        if (participantRepo.existsByChallengeIdAndUserId(challengeId, user.getId()))
            throw new IllegalArgumentException("Вы уже участвуете");

        ChallengeParticipant cp = new ChallengeParticipant();
        cp.setChallenge(ch);
        cp.setUser(user);
        participantRepo.save(cp);
    }

    @Transactional(readOnly = false)
    public void finalizeEndedUnratedChallenges() {
        LocalDateTime now = LocalDateTime.now();
        for (Long id : challengeRepo.findIdsUnratedEnded(now)) {
            finalizeChallengeById(id);
        }
    }

    @Transactional(readOnly = false)
    public void finalizeChallengeById(long challengeId) {
        Challenge ch = challengeRepo.findDetailById(challengeId).orElse(null);
        if (ch == null || ch.isRated()) return;
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(ch.getEndTime())) return;

        List<Long> taskIds = ch.getTasks().stream().map(Task::getId).toList();
        List<ChallengeParticipant> participants =
                participantRepo.findByChallengeIdOrderByTasksSolvedDesc(challengeId);

        List<long[]> standings = new ArrayList<>();
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

        Map<Long, Integer> deltas = standings.size() >= 2
                ? calculateEloDeltas(standings) : new HashMap<>();

        if (!deltas.isEmpty()) {
            for (long[] row : standings) {
                User u = userMap.get(row[0]);
                int d = deltas.getOrDefault(u.getId(), 0);
                u.setRating(Math.max(0, u.getRating() + d));
                userRepo.save(u);
            }
        }
        for (ChallengeParticipant cp : participants) {
            cp.setEloDelta(deltas.getOrDefault(cp.getUser().getId(), 0));
            participantRepo.save(cp);
        }
        ch.setRated(true);
        challengeRepo.save(ch);
        notifyChallengeEnded(standings, userMap, deltas, ch.getTitle(), ch);
    }

    @Transactional(readOnly = false)
    public List<ChallengeResultDto> getResults(Long challengeId) {
        finalizeChallengeById(challengeId);
        Challenge ch = challengeRepo.findDetailById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));
        List<Long> taskIds = ch.getTasks().stream().map(Task::getId).toList();
        List<ChallengeParticipant> participants =
                participantRepo.findByChallengeIdOrderByTasksSolvedDesc(challengeId);

        List<long[]> standings = new ArrayList<>();
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

        Map<Long, Integer> deltas;
        if (ch.isRated()) {
            deltas = new HashMap<>();
            for (ChallengeParticipant cp : participants) {
                deltas.put(cp.getUser().getId(), cp.getEloDelta());
            }
        } else {
            deltas = standings.size() >= 2 ? calculateEloDeltas(standings) : new HashMap<>();
        }

        AtomicInteger rank = new AtomicInteger(1);
        return standings.stream().map(s -> {
            User u = userMap.get(s[0]);
            return new ChallengeResultDto(rank.getAndIncrement(), u.getUsername(),
                    (int) s[2], u.getRating(), deltas.getOrDefault(u.getId(), 0));
        }).toList();
    }

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

    private int contestXpEarned(Challenge ch, User user) {
        LocalDateTime start = ch.getStartTime();
        LocalDateTime end = ch.getEndTime();
        List<Long> sortedIds = ch.getTasks().stream().map(Task::getId).sorted().toList();
        int n = sortedIds.size();
        if (n == 0) return 0;
        int bonusBase = ch.getBonusXp() / n;
        int bonusRem = ch.getBonusXp() % n;
        Map<Long, Task> byId = new HashMap<>();
        for (Task t : ch.getTasks()) {
            byId.put(t.getId(), t);
        }
        int total = 0;
        for (int pos = 0; pos < sortedIds.size(); pos++) {
            Long tid = sortedIds.get(pos);
            List<Submission> subs = submissionRepo.findByUserAndTaskAndStatusOrderBySubmittedAtAsc(
                    user.getId(), tid, SubmissionStatus.CORRECT);
            if (subs.isEmpty()) continue;
            Submission first = subs.get(0);
            LocalDateTime ts = first.getSubmittedAt();
            if (ts.isBefore(start) || ts.isAfter(end)) continue;
            Task task = byId.get(tid);
            if (task == null) continue;
            int share = bonusBase + (pos < bonusRem ? 1 : 0);
            total += task.getXpReward() + share;
        }
        return total;
    }

    private void notifyChallengeEnded(List<long[]> standings, Map<Long, User> userMap,
                                      Map<Long, Integer> deltas, String title, Challenge ch) {
        String head = "«" + title + "»: итоги";
        for (long[] row : standings) {
            User u = userMap.get(row[0]);
            int d = deltas.getOrDefault(u.getId(), 0);
            int xp = contestXpEarned(ch, u);
            int newR = u.getRating();
            int oldR = newR - d;
            String eloLine = d == 0
                    ? "Эло без изменений"
                    : String.format("Эло %+d (%d → %d)", d, oldR, newR);
            notificationService.notifyUser(u.getId(), head,
                    eloLine + ". Опыт по задачам соревнования: " + xp + " XP.");
        }
    }

    private ChallengeDto toDto(Challenge c, Long userId, LocalDateTime now) {
        boolean joined = userId != null &&
                participantRepo.existsByChallengeIdAndUserId(c.getId(), userId);
        boolean active = !now.isBefore(c.getStartTime()) && !now.isAfter(c.getEndTime());
        boolean upcoming = now.isBefore(c.getStartTime());
        List<Long> taskIds = List.of();
        if (userId != null && participantRepo.existsByChallengeIdAndUserId(c.getId(), userId)) {
            taskIds = c.getTasks().stream().map(Task::getId).sorted().toList();
        }
        return new ChallengeDto(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.getBonusXp(),
                c.getCreatedBy().getUsername(), c.getTasks().size(), joined, active, upcoming, taskIds);
    }
}

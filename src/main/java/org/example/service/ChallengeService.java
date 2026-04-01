package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepo;
    private final ChallengeParticipantRepository participantRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;

    public ChallengeService(ChallengeRepository challengeRepo,
                            ChallengeParticipantRepository participantRepo,
                            TaskRepository taskRepo, UserRepository userRepo,
                            SubmissionRepository submissionRepo) {
        this.challengeRepo = challengeRepo;
        this.participantRepo = participantRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
    }

    public List<ChallengeDto> findActive(String username) {
        Long userId = userRepo.findByUsername(username).map(User::getId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        return challengeRepo.findByEndTimeAfterOrderByStartTimeAsc(now).stream()
                .map(c -> toDto(c, userId, now))
                .toList();
    }

    public List<ChallengeDto> findAll(String username) {
        Long userId = userRepo.findByUsername(username).map(User::getId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        return challengeRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> toDto(c, userId, now))
                .toList();
    }

    @Transactional
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

        List<Task> tasks = taskRepo.findAllById(req.taskIds());
        ch.setTasks(tasks);
        challengeRepo.save(ch);

        return toDto(ch, author.getId(), LocalDateTime.now());
    }

    @Transactional
    public void join(Long challengeId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Challenge ch = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));

        if (participantRepo.existsByChallengeIdAndUserId(challengeId, user.getId())) {
            throw new IllegalArgumentException("Вы уже участвуете");
        }

        ChallengeParticipant cp = new ChallengeParticipant();
        cp.setChallenge(ch);
        cp.setUser(user);
        participantRepo.save(cp);
    }

    public List<ChallengeResultDto> getResults(Long challengeId) {
        Challenge ch = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Челлендж не найден"));

        List<ChallengeParticipant> participants =
                participantRepo.findByChallengeIdOrderByTasksSolvedDesc(challengeId);

        List<Long> taskIds = ch.getTasks().stream().map(Task::getId).toList();
        AtomicInteger rank = new AtomicInteger(1);

        return participants.stream().map(cp -> {
            int solved = 0;
            for (Long taskId : taskIds) {
                if (submissionRepo.existsByUserIdAndTaskIdAndStatus(
                        cp.getUser().getId(), taskId, SubmissionStatus.CORRECT)) {
                    solved++;
                }
            }
            return new ChallengeResultDto(rank.getAndIncrement(),
                    cp.getUser().getUsername(), solved, cp.getUser().getLevel());
        }).toList();
    }

    private ChallengeDto toDto(Challenge c, Long userId, LocalDateTime now) {
        boolean joined = userId != null &&
                participantRepo.existsByChallengeIdAndUserId(c.getId(), userId);
        boolean active = now.isAfter(c.getStartTime()) && now.isBefore(c.getEndTime());
        return new ChallengeDto(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.getBonusXp(),
                c.getCreatedBy().getUsername(), c.getTasks().size(),
                joined, active
        );
    }
}

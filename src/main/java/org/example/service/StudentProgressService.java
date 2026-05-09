package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentProgressService {

    private final UserRepository userRepo;
    private final TaskRepository taskRepo;
    private final LessonRepository lessonRepo;
    private final SubmissionRepository submissionRepo;
    private final GroupInviteRepository inviteRepo;
    private final LessonTaskRepository lessonTaskRepo;

    public StudentProgressService(UserRepository userRepo, TaskRepository taskRepo,
                                  LessonRepository lessonRepo, SubmissionRepository submissionRepo,
                                  GroupInviteRepository inviteRepo, LessonTaskRepository lessonTaskRepo) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
        this.inviteRepo = inviteRepo;
        this.lessonTaskRepo = lessonTaskRepo;
    }

    public List<StudentProgressDto> listGroup(String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        long totalTasks = taskRepo.count();
        return userRepo.findByRoleAndTeacherIdOrderByUsernameAsc(Role.STUDENT, teacher.getId()).stream()
                .map(u -> {
                    long solved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                            u.getId(), SubmissionStatus.CORRECT);
                    double pct = totalTasks > 0 ? (solved * 100.0 / totalTasks) : 0;
                    StudyGroup g = u.getStudyGroup();
                    return new StudentProgressDto(u.getId(), u.getUsername(),
                            u.getXp(), u.getLevel(), solved, totalTasks, Math.round(pct * 10) / 10.0,
                            g != null ? g.getId() : null,
                            g != null ? g.getName() : null);
                })
                .toList();
    }

    public List<StudentSummaryDto> listAvailable(String teacherUsername, String search) {
        User teacher = requireTeacher(teacherUsername);
        String q = search == null ? "" : search.trim().toLowerCase();
        return userRepo.findByRoleOrderByUsernameAsc(Role.STUDENT).stream()
                .filter(u -> {
                    User t = u.getTeacher();
                    return t == null || !t.getId().equals(teacher.getId());
                })
                .filter(u -> q.isEmpty() || u.getUsername().toLowerCase().contains(q))
                .map(u -> new StudentSummaryDto(
                        u.getId(),
                        u.getUsername(),
                        u.getTeacher() != null ? u.getTeacher().getUsername() : null,
                        inviteRepo.existsByTeacherIdAndStudentIdAndStatus(
                                teacher.getId(), u.getId(),
                                org.example.entity.GroupInvite.Status.PENDING)))
                .toList();
    }

    public StudentDetailDto getDetail(Long userId, String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        if (user.getRole() != Role.STUDENT
                || user.getTeacher() == null
                || !user.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Студент не из вашей группы");
        }

        long totalSolved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);

        List<LessonProgressDto> lessons = lessonRepo.findByAuthorIdOrderByOrderIndexAsc(teacher.getId()).stream()
                .map(lesson -> {
                    List<Task> tasks = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId()).stream()
                            .map(LessonTask::getTask).toList();
                    List<TaskProgressDto> taskProgress = tasks.stream().map(t -> {
                        boolean solved = submissionRepo.existsByUserIdAndTaskIdAndStatus(
                                user.getId(), t.getId(), SubmissionStatus.CORRECT);
                        long attempts = submissionRepo.findByUserIdAndTaskIdOrderBySubmittedAtDesc(
                                user.getId(), t.getId()).size();
                        return new TaskProgressDto(t.getId(), t.getTitle(),
                                t.getDifficulty().name(), solved, attempts);
                    }).toList();
                    int solved = (int) taskProgress.stream().filter(TaskProgressDto::solved).count();
                    return new LessonProgressDto(lesson.getId(), lesson.getTitle(),
                            solved, tasks.size(), taskProgress);
                }).toList();

        return new StudentDetailDto(
                user.getId(), user.getUsername(), user.getXp(), user.getLevel(),
                totalSolved, lessons);
    }

    @Transactional
    public void removeFromGroup(Long studentId, String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Студент не в вашей группе");
        }
        student.setTeacher(null);
        student.setStudyGroup(null);
    }

    private User requireTeacher(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (user.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Доступно только преподавателям");
        }
        return user;
    }
}

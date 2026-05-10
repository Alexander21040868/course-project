package org.example.service;

import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final StudentTeacherRepository studentTeacherRepo;
    private final TaskVisibilityService taskVisibility;

    public StudentProgressService(UserRepository userRepo, TaskRepository taskRepo,
                                  LessonRepository lessonRepo, SubmissionRepository submissionRepo,
                                  GroupInviteRepository inviteRepo, LessonTaskRepository lessonTaskRepo,
                                  StudentTeacherRepository studentTeacherRepo,
                                  TaskVisibilityService taskVisibility) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.lessonRepo = lessonRepo;
        this.submissionRepo = submissionRepo;
        this.inviteRepo = inviteRepo;
        this.lessonTaskRepo = lessonTaskRepo;
        this.studentTeacherRepo = studentTeacherRepo;
        this.taskVisibility = taskVisibility;
    }

    public List<StudentProgressDto> listGroup(String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        long totalTasks = taskRepo.countReleasedAt(LocalDateTime.now());
        return studentTeacherRepo.findStudentsByTeacherId(teacher.getId(), Role.STUDENT).stream()
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
                .filter(u -> !studentTeacherRepo.existsByStudentIdAndTeacherId(u.getId(), teacher.getId()))
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
        if (!studentTeacherRepo.existsByStudentIdAndTeacherId(user.getId(), teacher.getId())) {
            throw new IllegalArgumentException("Студент не из вашей группы");
        }

        long totalSolved = submissionRepo.countDistinctTaskByUserIdAndStatus(
                user.getId(), SubmissionStatus.CORRECT);

        List<LessonProgressDto> lessons = lessonRepo.findByAuthorIdOrderByOrderIndexAsc(teacher.getId()).stream()
                .map(lesson -> {
                    LocalDateTime now = LocalDateTime.now();
                    List<Task> tasks = lessonTaskRepo.findByLessonIdOrderByOrderIndexAsc(lesson.getId()).stream()
                            .map(LessonTask::getTask)
                            .filter(t -> taskVisibility.canView(t, user, now, lesson))
                            .toList();
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
                            solved, tasks.size(), taskProgress, teacher.getUsername(), lesson.getOrderIndex());
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
        if (!studentTeacherRepo.existsByStudentIdAndTeacherId(student.getId(), teacher.getId())) {
            throw new IllegalArgumentException("Студент не в вашей группе");
        }
        studentTeacherRepo.deleteByStudentIdAndTeacherId(student.getId(), teacher.getId());
        if (student.getTeacher() != null && student.getTeacher().getId().equals(teacher.getId())) {
            var next = studentTeacherRepo.findTeachersByStudentId(student.getId());
            student.setTeacher(next.isEmpty() ? null : next.get(0));
        }
        if (student.getStudyGroup() != null
                && student.getStudyGroup().getTeacher().getId().equals(teacher.getId())) {
            student.setStudyGroup(null);
        }
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

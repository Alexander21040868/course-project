package org.example.service;

import org.example.dto.GroupInviteDto;
import org.example.entity.GroupInvite;
import org.example.entity.Role;
import org.example.entity.StudyGroup;
import org.example.entity.StudentTeacherLink;
import org.example.entity.User;
import org.example.repository.GroupInviteRepository;
import org.example.repository.StudyGroupRepository;
import org.example.repository.UserRepository;
import org.example.repository.StudentTeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GroupInviteService {

    private final GroupInviteRepository inviteRepo;
    private final StudyGroupRepository groupRepo;
    private final UserRepository userRepo;
    private final NotificationService notifications;
    private final StudentTeacherRepository studentTeacherRepo;

    public GroupInviteService(GroupInviteRepository inviteRepo, StudyGroupRepository groupRepo,
                              UserRepository userRepo, NotificationService notifications,
                              StudentTeacherRepository studentTeacherRepo) {
        this.inviteRepo = inviteRepo;
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
        this.notifications = notifications;
        this.studentTeacherRepo = studentTeacherRepo;
    }

    @Transactional
    public void invite(Long studentId, Long groupId, String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        if (student.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Можно приглашать только студентов");
        }
        if (studentTeacherRepo.existsByStudentIdAndTeacherId(student.getId(), teacher.getId())) {
            throw new IllegalArgumentException("Студент уже в вашей группе");
        }
        if (inviteRepo.existsByTeacherIdAndStudentIdAndStatus(teacher.getId(), student.getId(), GroupInvite.Status.PENDING)) {
            throw new IllegalArgumentException("Приглашение уже отправлено");
        }

        StudyGroup group = null;
        if (groupId != null) {
            group = groupRepo.findByIdAndTeacherId(groupId, teacher.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Подгруппа не найдена"));
        }

        inviteRepo.save(new GroupInvite(teacher, student, group));

        String groupTag = group != null ? " (" + group.getName() + ")" : "";
        notifications.notifyUser(student.getId(),
                "Приглашение в группу",
                "Учитель " + teacher.getUsername() + " приглашает вас в свою группу" + groupTag + ". Откройте раздел «Приглашения», чтобы ответить.");
    }

    public List<GroupInviteDto> incoming(String studentUsername) {
        User student = userRepo.findByUsername(studentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return inviteRepo.findByStudentIdAndStatusOrderByCreatedAtDesc(student.getId(), GroupInvite.Status.PENDING).stream()
                .map(this::toDto)
                .toList();
    }

    public long pendingCount(String studentUsername) {
        return userRepo.findByUsername(studentUsername)
                .map(u -> inviteRepo.countByStudentIdAndStatus(u.getId(), GroupInvite.Status.PENDING))
                .orElse(0L);
    }

    @Transactional
    public void accept(Long inviteId, String studentUsername) {
        User student = userRepo.findByUsername(studentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        GroupInvite invite = inviteRepo.findByIdAndStudentId(inviteId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));
        if (invite.getStatus() != GroupInvite.Status.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }

        invite.setStatus(GroupInvite.Status.ACCEPTED);
        if (!studentTeacherRepo.existsByStudentIdAndTeacherId(student.getId(), invite.getTeacher().getId())) {
            studentTeacherRepo.save(new StudentTeacherLink(student.getId(), invite.getTeacher().getId()));
        }
        student.setTeacher(invite.getTeacher());
        student.setStudyGroup(invite.getGroup());

        notifications.notifyUser(invite.getTeacher().getId(),
                "Ученик принял приглашение",
                student.getUsername() + " теперь в вашей группе.");
    }

    @Transactional
    public void decline(Long inviteId, String studentUsername) {
        User student = userRepo.findByUsername(studentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        GroupInvite invite = inviteRepo.findByIdAndStudentId(inviteId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));
        if (invite.getStatus() != GroupInvite.Status.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }
        invite.setStatus(GroupInvite.Status.DECLINED);

        notifications.notifyUser(invite.getTeacher().getId(),
                "Ученик отклонил приглашение",
                student.getUsername() + " отклонил(а) ваше приглашение.");
    }

    private GroupInviteDto toDto(GroupInvite invite) {
        return new GroupInviteDto(
                invite.getId(),
                invite.getTeacher().getUsername(),
                invite.getGroup() != null ? invite.getGroup().getName() : null,
                invite.getCreatedAt());
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

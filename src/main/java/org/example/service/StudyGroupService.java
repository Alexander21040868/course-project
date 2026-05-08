package org.example.service;

import org.example.dto.StudyGroupDto;
import org.example.entity.Role;
import org.example.entity.StudyGroup;
import org.example.entity.User;
import org.example.repository.StudyGroupRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudyGroupService {

    private final StudyGroupRepository groupRepo;
    private final UserRepository userRepo;

    public StudyGroupService(StudyGroupRepository groupRepo, UserRepository userRepo) {
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
    }

    public List<StudyGroupDto> listForTeacher(String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        return groupRepo.findByTeacherIdOrderByCreatedAtAsc(teacher.getId()).stream()
                .map(g -> new StudyGroupDto(g.getId(), g.getName(),
                        userRepo.countByRoleAndStudyGroupId(Role.STUDENT, g.getId())))
                .toList();
    }

    @Transactional
    public StudyGroupDto create(String teacherUsername, String name) {
        User teacher = requireTeacher(teacherUsername);
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Название группы пустое");
        if (trimmed.length() > 120) throw new IllegalArgumentException("Слишком длинное название");
        if (groupRepo.existsByTeacherIdAndNameIgnoreCase(teacher.getId(), trimmed)) {
            throw new IllegalArgumentException("Группа с таким именем уже есть");
        }
        StudyGroup g = groupRepo.save(new StudyGroup(teacher, trimmed));
        return new StudyGroupDto(g.getId(), g.getName(), 0);
    }

    @Transactional
    public void delete(Long groupId, String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        StudyGroup group = groupRepo.findByIdAndTeacherId(groupId, teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));
        userRepo.findByRoleAndStudyGroupIdOrderByUsernameAsc(Role.STUDENT, group.getId())
                .forEach(u -> u.setStudyGroup(null));
        groupRepo.delete(group);
    }

    @Transactional
    public void moveStudent(Long studentId, Long groupId, String teacherUsername) {
        User teacher = requireTeacher(teacherUsername);
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Студент не из вашей группы");
        }
        if (groupId == null) {
            student.setStudyGroup(null);
            return;
        }
        StudyGroup group = groupRepo.findByIdAndTeacherId(groupId, teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Подгруппа не найдена"));
        student.setStudyGroup(group);
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

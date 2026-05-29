package org.example.repository;

import org.example.entity.Role;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleOrderByUsernameAsc(Role role);
    List<User> findByRoleAndTeacherIdOrderByUsernameAsc(Role role, Long teacherId);
    List<User> findByRoleAndTeacherIsNullOrderByUsernameAsc(Role role);
    List<User> findByRoleAndStudyGroupIdOrderByUsernameAsc(Role role, Long groupId);
    long countByRoleAndStudyGroupId(Role role, Long groupId);
    List<User> findTop50ByOrderByXpDesc();
    List<User> findTop50ByOrderByRatingDesc();
}

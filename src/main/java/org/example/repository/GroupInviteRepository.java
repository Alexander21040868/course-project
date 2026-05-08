package org.example.repository;

import org.example.entity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    List<GroupInvite> findByStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, GroupInvite.Status status);

    Optional<GroupInvite> findByIdAndStudentId(Long id, Long studentId);

    boolean existsByTeacherIdAndStudentIdAndStatus(Long teacherId, Long studentId, GroupInvite.Status status);

    long countByStudentIdAndStatus(Long studentId, GroupInvite.Status status);
}

package org.example.repository;

import org.example.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    List<StudyGroup> findByTeacherIdOrderByCreatedAtAsc(Long teacherId);

    Optional<StudyGroup> findByIdAndTeacherId(Long id, Long teacherId);

    boolean existsByTeacherIdAndNameIgnoreCase(Long teacherId, String name);

    long countByTeacherId(Long teacherId);
}

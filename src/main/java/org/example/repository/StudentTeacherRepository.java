package org.example.repository;

import org.example.entity.Role;
import org.example.entity.StudentTeacherLink;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentTeacherRepository extends JpaRepository<StudentTeacherLink, StudentTeacherLink.Key> {

    boolean existsByStudentIdAndTeacherId(Long studentId, Long teacherId);

    void deleteByStudentIdAndTeacherId(Long studentId, Long teacherId);

    @Query("select t from StudentTeacherLink st join User t on t.id = st.teacherId where st.studentId = :sid order by t.username asc")
    List<User> findTeachersByStudentId(@Param("sid") Long sid);

    @Query("select s from StudentTeacherLink st join User s on s.id = st.studentId where st.teacherId = :tid and s.role = :studentRole order by s.username asc")
    List<User> findStudentsByTeacherId(@Param("tid") Long tid, @Param("studentRole") Role studentRole);
}

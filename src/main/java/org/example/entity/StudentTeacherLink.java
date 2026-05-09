package org.example.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "student_teachers")
@IdClass(StudentTeacherLink.Key.class)
public class StudentTeacherLink {

    @Id
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Id
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private User teacher;

    public StudentTeacherLink() {}

    public StudentTeacherLink(Long studentId, Long teacherId) {
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public User getStudent() { return student; }
    public User getTeacher() { return teacher; }

    public static class Key implements Serializable {
        private Long studentId;
        private Long teacherId;

        public Key() {}

        public Key(Long studentId, Long teacherId) {
            this.studentId = studentId;
            this.teacherId = teacherId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(studentId, key.studentId) && Objects.equals(teacherId, key.teacherId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, teacherId);
        }
    }
}

package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_invites")
public class GroupInvite {

    public enum Status { PENDING, ACCEPTED, DECLINED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private StudyGroup group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GroupInvite() {}

    public GroupInvite(User teacher, User student, StudyGroup group) {
        this.teacher = teacher;
        this.student = student;
        this.group = group;
    }

    public Long getId() { return id; }

    public User getTeacher() { return teacher; }
    public User getStudent() { return student; }

    public StudyGroup getGroup() { return group; }
    public void setGroup(StudyGroup group) { this.group = group; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

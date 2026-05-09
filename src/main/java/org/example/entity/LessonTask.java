package org.example.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "lesson_tasks")
public class LessonTask {

    @EmbeddedId
    private LessonTaskId id = new LessonTaskId();

    @MapsId("lessonId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public LessonTask() {}

    public LessonTask(Lesson lesson, Task task, int orderIndex) {
        this.lesson = lesson;
        this.task = task;
        this.orderIndex = orderIndex;
        this.id = new LessonTaskId(lesson.getId(), task.getId());
    }

    public LessonTaskId getId() { return id; }

    public Lesson getLesson() { return lesson; }
    public Task getTask() { return task; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    @Embeddable
    public static class LessonTaskId implements Serializable {
        @Column(name = "lesson_id")
        private Long lessonId;

        @Column(name = "task_id")
        private Long taskId;

        public LessonTaskId() {}

        public LessonTaskId(Long lessonId, Long taskId) {
            this.lessonId = lessonId;
            this.taskId = taskId;
        }

        public Long getLessonId() { return lessonId; }
        public Long getTaskId() { return taskId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LessonTaskId other)) return false;
            return Objects.equals(lessonId, other.lessonId) && Objects.equals(taskId, other.taskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lessonId, taskId);
        }
    }
}

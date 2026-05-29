CREATE TABLE student_teachers (
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, teacher_id),
    CONSTRAINT fk_st_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_st_teacher ON student_teachers(teacher_id);

INSERT INTO student_teachers (student_id, teacher_id)
SELECT id, teacher_id FROM users WHERE teacher_id IS NOT NULL AND role = 'STUDENT';

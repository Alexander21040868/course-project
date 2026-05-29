ALTER TABLE users ADD COLUMN teacher_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);

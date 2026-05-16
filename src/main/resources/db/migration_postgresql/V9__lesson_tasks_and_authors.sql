CREATE TABLE lesson_tasks (
    lesson_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    PRIMARY KEY (lesson_id, task_id),
    CONSTRAINT fk_lt_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT fk_lt_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_lt_lesson ON lesson_tasks(lesson_id, order_index);
CREATE INDEX idx_lt_task ON lesson_tasks(task_id);

INSERT INTO lesson_tasks (lesson_id, task_id, order_index)
SELECT lesson_id, id, order_index FROM tasks WHERE lesson_id IS NOT NULL;

ALTER TABLE tasks ALTER COLUMN lesson_id DROP NOT NULL;

ALTER TABLE tasks ADD COLUMN author_id BIGINT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_author FOREIGN KEY (author_id) REFERENCES users(id);

UPDATE tasks t SET author_id = (SELECT l.author_id FROM lessons l WHERE l.id = t.lesson_id)
    WHERE t.lesson_id IS NOT NULL;

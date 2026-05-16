ALTER TABLE tasks ADD COLUMN catalog_visible_from TIMESTAMP NULL;

UPDATE tasks SET catalog_visible_from = (
    SELECT MIN(c.start_time)
    FROM challenge_tasks ct
    INNER JOIN challenges c ON c.id = ct.challenge_id
    WHERE ct.task_id = tasks.id
) WHERE EXISTS (
    SELECT 1 FROM challenge_tasks ct2 WHERE ct2.task_id = tasks.id
);

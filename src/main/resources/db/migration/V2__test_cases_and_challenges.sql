CREATE TABLE test_cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    input           CLOB,
    expected_output CLOB         NOT NULL,
    is_sample       BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index     INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE challenges (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    start_time   TIMESTAMP     NOT NULL,
    end_time     TIMESTAMP     NOT NULL,
    bonus_xp     INT           NOT NULL DEFAULT 50,
    created_by   BIGINT        NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE challenge_tasks (
    challenge_id BIGINT NOT NULL,
    task_id      BIGINT NOT NULL,
    PRIMARY KEY (challenge_id, task_id),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);

CREATE TABLE challenge_participants (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id  BIGINT    NOT NULL,
    user_id       BIGINT    NOT NULL,
    joined_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tasks_solved  INT       NOT NULL DEFAULT 0,
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (challenge_id, user_id)
);

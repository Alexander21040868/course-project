CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(120) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    xp          INT          NOT NULL DEFAULT 0,
    level       INT          NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lessons (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    content     CLOB,
    order_index INT          NOT NULL DEFAULT 0,
    author_id   BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id       BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     CLOB,
    difficulty      VARCHAR(10)  NOT NULL DEFAULT 'EASY',
    xp_reward       INT          NOT NULL DEFAULT 10,
    template_code   CLOB,
    expected_output VARCHAR(2000),
    hints           VARCHAR(2000),
    order_index     INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id)
);

CREATE TABLE submissions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    task_id      BIGINT      NOT NULL,
    code         CLOB        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    output       CLOB,
    submitted_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);

CREATE TABLE achievements (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon        VARCHAR(10)  NOT NULL DEFAULT '🏆',
    xp_reward   INT          NOT NULL DEFAULT 0,
    threshold   INT          NOT NULL DEFAULT 1
);

CREATE TABLE user_achievements (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT    NOT NULL,
    achievement_id BIGINT    NOT NULL,
    earned_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)        REFERENCES users(id),
    FOREIGN KEY (achievement_id) REFERENCES achievements(id),
    UNIQUE (user_id, achievement_id)
);

INSERT INTO achievements (code, name, description, icon, xp_reward, threshold) VALUES
    ('FIRST_BLOOD',  'Первая кровь',     'Решите свою первую задачу',      '⚔️', 50,  1),
    ('SOLVER_10',    'Подмастерье',       'Решите 10 задач',               '🔧', 100, 10),
    ('SOLVER_50',    'Мастер кода',       'Решите 50 задач',               '👨‍💻', 300, 50),
    ('PERFECT_5',    'Без единой ошибки', '5 задач подряд с первой попытки','✨', 150, 5),
    ('SPEEDRUN',     'Спидраннер',        'Решите задачу за 60 секунд',    '⚡', 75,  1),
    ('ALL_EASY',     'Разминка окончена', 'Решите все лёгкие задачи',      '🟢', 200, 1),
    ('HARD_FIRST',   'Бесстрашный',      'Решите сложную задачу первой',   '🔥', 120, 1),
    ('LESSON_CLEAR', 'Зачистка подземелья','Пройдите все задачи урока',    '🏰', 100, 1);

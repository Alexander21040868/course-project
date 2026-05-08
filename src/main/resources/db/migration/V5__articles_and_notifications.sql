CREATE TABLE articles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     CLOB         NOT NULL,
    category    VARCHAR(80)  NOT NULL DEFAULT 'Справочник',
    order_index INT          NOT NULL DEFAULT 0,
    author_id   BIGINT,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    read_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- INSERT INTO articles (title, content, category, order_index) VALUES
-- ('Типы данных в C', '## Основные типы\n\n- `int` — целые\n- `float`, `double` — вещественные\n- `char` — символ\n- `void` — «ничего» (для функций)\n\nИспользуйте `%d` для `printf` с целыми и `%f` для дробных.', 'Справочник', 1),
-- ('Частая ошибка: точка с запятой', 'Компилятор C требует `;` после каждого **выражения** в теле функции. Забытая точка с запятой после `printf` или `return` даёт каскад странных ошибок.', 'Частые ошибки', 2),
-- ('Лайфхак: форматирование вывода', 'Для выравнивания столбцов используйте ширину в `printf`, например `printf("%5d", x);`. Для отладки удобно выводить промежуточные значения.', 'Лайфхаки', 3);

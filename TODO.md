# CodeQuest — Дорожная карта развития

> Статус проекта: **MVP готов** (базовые уроки, задачи, XP, достижения, JWT-авторизация).
> Цель документа: подробный план доработок до полноценной курсовой работы.

Каждая задача имеет **приоритет**, **оценку трудоёмкости** и **подробное описание**
того, что именно нужно сделать — вплоть до конкретных файлов и методов.

---

## Обозначения

| Значок | Приоритет | Смысл |
|--------|-----------|-------|
| 🔴 | Критично | Без этого проект не примут как курсовую |
| 🟠 | Важно | Сильно улучшает продукт, явно упомянуто в ТЗ |
| 🟡 | Желательно | Делает проект зрелым и целостным |
| 🟢 | Бонус | Вау-фактор на защите |

| Оценка | Примерное время |
|--------|-----------------|
| S | 1–2 часа |
| M | 3–5 часов |
| L | 6–12 часов |
| XL | 1–2 дня |

---

## Фаза 1 — Закрытие требований ТЗ

Всё, что **явно указано** в исходном документе, но ещё не реализовано.

---

### 1.1 🔴 Множественные тест-кейсы для задач [L]

**Сейчас:** у задачи одно поле `expected_output`, проверка — наивный поиск подстрок.
**Надо:** у задачи несколько тест-кейсов (вход → ожидаемый выход), проверка — сравнение
выхода программы с эталоном по каждому тесту.

**Что делать:**

1. Новая миграция `V2__test_cases.sql`:

```sql
CREATE TABLE test_cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    input           CLOB,
    expected_output CLOB         NOT NULL,
    is_sample       BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index     INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);
```

2. Entity `TestCase.java` (`task` — `@ManyToOne`, поля `input`, `expectedOutput`, `isSample`).
3. `TestCaseRepository` — `findByTaskIdOrderByOrderIndexAsc(Long taskId)`.
4. DTO `TestCaseDto` (id, input, expectedOutput, isSample).
5. В `SubmissionService.checkSolution()` — вместо наивной проверки: итерация по тест-кейсам,
   сравнение `actualOutput.trim()` с `expectedOutput.trim()` по каждому. Результат — «3 из 5 тестов пройдено».
6. В `SubmissionResponse` добавить `int passedTests, int totalTests`.
7. В `TaskCreateRequest` — массив тест-кейсов; в `AdminController` — сохранение тестов вместе с задачей.
8. На фронтенде: студенту показывать sample-тесты (с `isSample = true`), скрытые тесты — не показывать.
   В результате решения — показывать «Пройдено 3/5 тестов», для провалившегося — показать input и ожидаемый выход sample-теста.

**Зачем:** критерий из ТЗ — «Качество проверки решений задач: доля задач, для которых реализованы
автоматические тесты, корректно определяющие результат».

---

### 1.2 🔴 Просмотр прогресса студентов (панель преподавателя) [L]

**Сейчас:** преподаватель может только создавать и удалять уроки/задачи.
**Надо:** преподаватель видит список студентов, их XP, уровень, процент решённых задач,
может открыть конкретного студента и увидеть по каждому уроку — какие задачи решены.

**Что делать:**

1. DTO `StudentProgressDto`:

```java
public record StudentProgressDto(
    Long userId, String username, int xp, int level,
    long totalSolved, long totalTasks, double solvedPercent
) {}
```

2. DTO `StudentLessonProgressDto`:

```java
public record StudentLessonProgressDto(
    Long lessonId, String lessonTitle,
    List<TaskProgressItem> tasks
) {}
public record TaskProgressItem(
    Long taskId, String title, String difficulty,
    boolean solved, int attempts
) {}
```

3. `AdminService` — методы:
   - `getAllStudentsProgress()` — список всех студентов с агрегированными метриками
   - `getStudentDetail(Long userId)` — по каждому уроку и задаче, решена ли, сколько попыток

4. `AdminController`:
   - `GET /api/admin/students` — список студентов с прогрессом
   - `GET /api/admin/students/{id}` — детальный прогресс конкретного студента

5. Фронтенд: в view-admin добавить вкладку «Студенты» с таблицей (username, XP, уровень,
   прогресс-бар решённых задач). По клику — детализация по урокам.

**Зачем:** ТЗ: «преподавателям — контролировать успеваемость», «просмотр прогресса».

---

### 1.3 🔴 Челленджи (соревновательные задачи с дедлайном) [XL]

**Сейчас:** не реализовано.
**Надо:** преподаватель создаёт челлендж — задача (или набор задач) с ограничением по времени.
Студенты видят активные челленджи, решают, получают бонусные XP.

**Что делать:**

1. Миграция `V3__challenges.sql`:

```sql
CREATE TABLE challenges (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    start_time   TIMESTAMP     NOT NULL,
    end_time     TIMESTAMP     NOT NULL,
    bonus_xp     INT           NOT NULL DEFAULT 50,
    created_by   BIGINT        NOT NULL,
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
    total_time_ms BIGINT    NOT NULL DEFAULT 0,
    FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (challenge_id, user_id)
);
```

2. Entities: `Challenge`, `ChallengeParticipant` (+ связь `@ManyToMany` для задач или отдельная join-таблица).
3. `ChallengeService`:
   - `create(ChallengeCreateRequest, username)` — создать (только TEACHER)
   - `findActive()` — челленджи, где `now()` между `startTime` и `endTime`
   - `join(challengeId, username)` — записаться
   - `getResults(challengeId)` — таблица результатов (кто сколько решил, за какое время)
4. `ChallengeController` — `/api/challenges`, `/api/challenges/{id}/join`, `/api/challenges/{id}/results`.
5. При решении задачи в `SubmissionService` — проверять, привязана ли задача к активному
   челленджу, обновлять `ChallengeParticipant.tasksSolved` и `totalTimeMs`.
6. Фронтенд: новый view «Арена» с карточками активных челленджей, таймером обратного
   отсчёта, таблицей результатов.

**Зачем:** ТЗ явно указывает «челленджи» как обязательный элемент геймификации и
«создание соревнований» в инструментах преподавателя.

---

### 1.4 🟠 Лидерборд (рейтинговая таблица) [M]

**Сейчас:** не реализовано.
**Надо:** таблица лучших студентов — по XP, по количеству решённых задач, по уровню.

**Что делать:**

1. `UserRepository` — добавить: `List<User> findTop50ByRoleOrderByXpDesc(Role role)`.
2. DTO `LeaderboardEntryDto(int rank, String username, int xp, int level, long solvedCount)`.
3. `LeaderboardService` (или метод в `GamificationService`):
   - `getLeaderboard()` — топ-50 студентов
   - Для каждого — count решённых задач через `SubmissionRepository`
4. `LeaderboardController` — `GET /api/leaderboard`.
5. Разрешить в `SecurityConfig`.
6. Фронтенд: view «Лидерборд» с таблицей, подсветкой текущего пользователя, медалями
   для топ-3 (🥇🥈🥉), анимацией при попадании в топ.

**Зачем:** ТЗ: «полноценный лидерборд по группе» в разделе перспектив.

---

### 1.5 🟠 Streak — серия дней активности [M]

**Сейчас:** не реализовано.
**Надо:** отслеживать, сколько дней подряд студент решает хотя бы одну задачу.
Бонусные XP за длинные серии. Отображение в профиле и сайдбаре.

**Что делать:**

1. Миграция `V4__streak.sql`:

```sql
ALTER TABLE users ADD COLUMN streak INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN max_streak INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_solved_date DATE;
```

2. В `User.java` — добавить поля `streak`, `maxStreak`, `lastSolvedDate`.
3. В `GamificationService` — метод `updateStreak(User user)`:
   - Если `lastSolvedDate == null` или < `вчера` → streak = 1
   - Если `lastSolvedDate == вчера` → streak++
   - Если `lastSolvedDate == сегодня` → ничего (уже активен)
   - Обновить `maxStreak` если текущий streak больше
   - Бонус: streak >= 7 → +25 XP, streak >= 30 → +100 XP
4. Вызывать `updateStreak()` в `SubmissionService.submit()` при верном ответе.
5. Добавить streak в `ProfileDto` и отобразить в сайдбаре: «🔥 5 дней подряд».
6. Новые достижения: «Неделя огня» (streak 7), «Месяц стали» (streak 30).

**Зачем:** ТЗ: «streak (серия дней)» явно упомянут.

---

### 1.6 🟠 Задача дня [M]

**Сейчас:** не реализовано.
**Надо:** каждый день автоматически выбирается одна задача, за решение которой —
двойные XP. Отображается на главной странице.

**Что делать:**

1. Логика выбора — детерминистическая (без состояния в БД): `taskId = (dayOfYear % totalTasks) + 1`
   или выбирать задачу, которую решило меньше всего людей.
2. `DailyTaskService`:
   - `getDailyTask()` → вычисляет ID задачи дня
   - `isDailyTask(Long taskId)` → проверка
3. `DailyTaskController` — `GET /api/daily-task` → возвращает `TaskDto` + `bonusXp`.
4. В `SubmissionService` — если задача совпадает с задачей дня и решена впервые → двойной XP.
5. Фронтенд: на главном экране — отдельная карточка «⚡ Задача дня» с таймером до полуночи.

**Зачем:** ТЗ: «автоматическая задача дня» в разделе перспектив.

---

## Фаза 2 — Личный кабинет и UX

Функции, которые превращают MVP в приятный для использования продукт.

---

### 2.1 🔴 Полноценный личный кабинет студента [L]

**Сейчас:** профиль — только XP/уровень/достижения в сайдбаре, нет отдельной страницы.
**Надо:** полноэкранная страница профиля с визуализацией прогресса.

**Содержание страницы:**

```
┌────────────────────────────────────────────────────────┐
│  [Аватар]  username                                    │
│  Роль: Искатель приключений   Уровень: 5               │
│  На платформе с: 15 марта 2026                         │
│                                                        │
│  ████████████████░░░░  1350 / 2500 XP до уровня 6     │
│                                                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 🔥 12    │ │ ⚔ 23     │ │ 📖 3/3   │ │ 🏆 5     │  │
│  │ Streak   │ │ Решено   │ │ Уроки    │ │ Ачивки   │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│                                                        │
│  [Карта прогресса по урокам — прогресс-бары]           │
│  Урок 1: Введение в C      ████████████ 3/3 (100%)    │
│  Урок 2: Условия и циклы   ██████░░░░░░ 1/2 (50%)     │
│  Урок 3: Массивы и строки  ░░░░░░░░░░░░ 0/2 (0%)      │
│                                                        │
│  [Последние решения]                                   │
│  12:34  Hello World           ✅ +15 XP                │
│  12:20  Сумма двух чисел      ❌ Неверно               │
│  11:50  Hello World           ❌ Неверно               │
│                                                        │
│  [Витрина достижений — сетка карточек]                 │
│  ⚔ Первая кровь  🔧 Подмастерье  🔒 Мастер кода       │
└────────────────────────────────────────────────────────┘
```

**Что делать:**

1. Расширить `ProfileDto` — добавить `createdAt`, `streak`, `maxStreak`, `lessonsProgress` (список
   {lessonId, title, solved, total}), `recentSubmissions` (последние 10).
2. Новый DTO `LessonProgressDto(Long lessonId, String title, int solved, int total)`.
3. В `ProfileController.getProfile()` — собрать всю статистику.
4. В `SubmissionRepository` — `findTop10ByUserIdOrderBySubmittedAtDesc(Long userId)`.
5. Фронтенд: отдельный view `view-profile` со всей визуализацией. Навигация из сайдбара.

---

### 2.2 🟠 История отправок [S]

**Сейчас:** студент не видит свои прошлые попытки.
**Надо:** на странице задачи — список предыдущих отправок (дата, статус, код).

**Что делать:**

1. DTO `SubmissionHistoryDto(Long id, String status, String output, LocalDateTime submittedAt)`.
2. В `SubmissionService`:
   ```java
   public List<SubmissionHistoryDto> getTaskHistory(Long taskId, String username) {
       User user = userRepo.findByUsername(username).orElseThrow(...);
       return submissionRepo.findByUserIdAndTaskIdOrderBySubmittedAtDesc(user.getId(), taskId)
               .stream().map(s -> new SubmissionHistoryDto(...)).toList();
   }
   ```
3. `SubmissionController` — `GET /api/submissions/task/{taskId}`.
4. Фронтенд: под редактором кода — сворачиваемый блок «Предыдущие попытки» с цветовым
   индикатором (зелёный/красный), временем, возможностью подставить код из старой попытки.

---

### 2.3 🟠 Редактирование уроков и задач (полный CRUD) [M]

**Сейчас:** преподаватель может создать и удалить, но не редактировать.
**Надо:** полный CRUD — создание, просмотр, редактирование, удаление.

**Что делать:**

1. В `LessonService` и `TaskService` — добавить методы `update(Long id, Request req)`.
2. В `AdminController`:
   - `PUT /api/admin/lessons/{id}` — обновить урок
   - `PUT /api/admin/tasks/{id}` — обновить задачу
3. Фронтенд: в админ-панели рядом с каждым уроком/задачей — кнопки «Редактировать» и
   «Удалить». При нажатии «Редактировать» — форма заполняется текущими данными.
4. Добавить список существующих уроков и задач в админ-панели (сейчас — только формы
   создания, нет обзора существующего контента).

---

### 2.4 🟡 Подсветка синтаксиса в редакторе кода [M]

**Сейчас:** `<textarea>` без подсветки.
**Надо:** подсветка синтаксиса C, нумерация строк.

**Варианты:**

- **CodeMirror 6** (рекомендуется) — подключается через CDN, ~100 КБ. Поддержка C.
  ```html
  <script src="https://cdn.jsdelivr.net/npm/codemirror@5.65.16/lib/codemirror.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/codemirror@5.65.16/mode/clike/clike.min.js"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/codemirror@5.65.16/lib/codemirror.min.css">
  ```
  Инициализация: `CodeMirror.fromTextArea(codeEditor, { mode: 'text/x-csrc', theme: 'material-darker' })`.

- **Ace Editor** — альтернатива, тоже через CDN.

- **Минимальный вариант (без библиотек):** `<pre><code>` с подсветкой через CSS-классы
  и regex-замены ключевых слов.

**Зачем:** не критерий оценки, но делает UX значительно приятнее и выглядит профессионально на защите.

---

### 2.5 🟡 Markdown в теории уроков [S]

**Сейчас:** содержание уроков хранится как сырой HTML.
**Надо:** преподаватель пишет в Markdown, приложение рендерит в HTML.

**Что делать:**

1. Подключить библиотеку `marked.js` через CDN:
   ```html
   <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
   ```
2. В `app.js` при отображении урока:
   ```javascript
   lessonContent.innerHTML = marked.parse(lesson.content);
   ```
3. Преподаватель в форме создания урока пишет:
   ```markdown
   ## Что такое C?
   C — один из старейших языков. Пример:
   ```c
   printf("Hello");
   ```
   ```
4. Бонус: подсветка кода внутри Markdown через `highlight.js`.

---

## Фаза 3 — Геймификация на максимум

Элементы, которые делают платформу **действительно** игровой, а не просто «сайтом с задачами».

---

### 3.1 🟠 Расширенная система достижений [M]

**Сейчас:** 8 достижений, проверяются только 3 (FIRST_BLOOD, SOLVER_10, SOLVER_50).
**Надо:** все 8+ достижений работают, добавить новые.

**Что делать:**

1. Реализовать проверку для оставшихся 5 достижений в `GamificationService`:

   - **PERFECT_5** — 5 задач подряд с первой попытки.
     Логика: при каждом верном ответе проверять, что для последних 5 решённых задач
     количество попыток == 1. Запрос: для каждой из последних 5 решённых задач —
     `countByUserIdAndTaskId()` должен быть == 1.

   - **SPEEDRUN** — задача за 60 секунд.
     Логика: нужно добавить поле `startedAt` (когда студент открыл задачу) в localStorage
     или в БД. При сабмите — сравнить с `submittedAt`. Проще всего: на фронтенде запомнить
     `Date.now()` при открытии задачи и передать в запросе.

   - **ALL_EASY** — все лёгкие задачи решены.
     Логика: `taskRepo.countByDifficulty(EASY)` == количество уникальных решённых EASY-задач.

   - **HARD_FIRST** — первая решённая задача — HARD.
     Логика: если `solvedCount == 1` и решённая задача имеет `difficulty == HARD`.

   - **LESSON_CLEAR** — все задачи одного урока решены.
     Логика: для каждого урока — количество задач == количество решённых.

2. Добавить новые достижения (миграция `V5__more_achievements.sql`):

   | Код | Название | Условие | XP |
   |-----|---------|---------|-----|
   | `STREAK_7` | Неделя огня | Streak 7 дней | +75 |
   | `STREAK_30` | Месяц стали | Streak 30 дней | +200 |
   | `NIGHT_OWL` | Полуночник | Решение после полуночи | +50 |
   | `EARLY_BIRD` | Ранняя пташка | Решение до 7 утра | +50 |
   | `CHALLENGER` | Гладиатор | Выиграть челлендж | +150 |
   | `COLLECTOR` | Коллекционер | Все достижения | +500 |

3. Фронтенд: на странице достижений показывать **все** достижения — полученные (цветные)
   и неполученные (серые/заблокированные) с описанием условий.

---

### 3.2 🟡 Система титулов / званий [S]

**Сейчас:** роль просто «Студент» / «Преподаватель».
**Надо:** студент получает звание в зависимости от уровня.

```
Уровень 1–2:  Новобранец
Уровень 3–4:  Ученик
Уровень 5–7:  Подмастерье
Уровень 8–10: Кодер
Уровень 11–15: Мастер
Уровень 16+:  Архимаг кода
```

**Реализация:** чистая логика на фронтенде (или один метод в `GamificationService`):

```java
public String getTitle(int level) {
    if (level >= 16) return "Архимаг кода";
    if (level >= 11) return "Мастер";
    if (level >= 8)  return "Кодер";
    // ...
}
```

Отображать в профиле, в сайдбаре, в лидерборде.

---

### 3.3 🟢 Анимация Level Up [S]

**Сейчас:** уровень тихо обновляется в сайдбаре.
**Надо:** при переходе на новый уровень — полноэкранная анимация «LEVEL UP!» на 2–3 секунды.

**Реализация:** на фронтенде. Сохранять `previousLevel` в `state`. После каждого `loadProfile()` —
сравнивать. Если `newLevel > previousLevel`:

```javascript
function showLevelUpAnimation(newLevel) {
    const overlay = document.createElement('div');
    overlay.className = 'levelup-overlay';
    overlay.innerHTML = `
        <div class="levelup-content">
            <div class="levelup-icon">⬆</div>
            <div class="levelup-text">LEVEL UP!</div>
            <div class="levelup-level">${newLevel}</div>
        </div>`;
    document.body.appendChild(overlay);
    setTimeout(() => overlay.remove(), 3000);
}
```

CSS: полупрозрачный оверлей с blur, золотой текст, pulse-анимация, частицы.

---

### 3.4 🟢 Звуковые эффекты [S]

Короткие звуки при: верном ответе, неверном ответе, получении достижения, level up.
Реализация через `new Audio('/sounds/success.mp3').play()`. Файлы — свободные 8-bit SFX.
Добавить кнопку mute в сайдбаре.

---

## Фаза 4 — Контент и качество проверки

---

### 4.1 🔴 Наполнение контентом (минимум 5 уроков, 20+ задач) [L]

**Сейчас:** 3 урока, 7 задач.
**Надо:** полноценный курс по основам C.

**Рекомендуемая программа:**

| # | Урок | Кол-во задач | Темы |
|---|------|-------------|------|
| 1 | Введение в C | 4 | Hello World, printf, переменные, типы |
| 2 | Условия и ветвления | 4 | if/else, switch, тернарный оператор |
| 3 | Циклы | 4 | for, while, do-while, break/continue |
| 4 | Функции | 4 | объявление, параметры, return, рекурсия |
| 5 | Массивы | 3 | одномерные, сортировка, поиск |
| 6 | Строки | 3 | char[], strlen, strcmp, strcpy |
| 7 | Указатели | 3 | &, *, арифметика указателей |
| 8 | Структуры | 3 | struct, typedef, массивы структур |

Итого: 8 уроков, 28 задач. Каждая задача — с описанием, шаблоном кода, 2–3 тест-кейсами, подсказкой.

**Где добавлять:** в `DataInitializer.java` или (лучше) через отдельную миграцию
`V6__seed_content.sql` с INSERT-запросами.

---

### 4.2 🟠 Компиляция и запуск C-кода в Docker-песочнице [XL]

**Сейчас:** проверка — наивное сравнение строк.
**Надо:** реальная компиляция GCC и запуск программы с stdin → сравнение stdout.

**Архитектура:**

```
SubmissionService
    │
    ▼
CodeExecutionService.execute(code, input, timeoutMs)
    │
    ▼
Docker API (или ProcessBuilder)
    │
    ▼
Контейнер: gcc:alpine
    1. Записать code → /tmp/solution.c
    2. gcc solution.c -o solution
    3. echo input | timeout 5s ./solution
    4. Захватить stdout и stderr
    5. Вернуть результат
```

**Варианты реализации (от простого к сложному):**

**Вариант А — ProcessBuilder (без Docker, проще):**

```java
@Service
public class CodeExecutionService {
    public ExecutionResult execute(String code, String input, long timeoutMs) {
        Path dir = Files.createTempDirectory("cq-");
        Path source = dir.resolve("solution.c");
        Files.writeString(source, code);

        // Компиляция
        Process gcc = new ProcessBuilder("gcc", source.toString(), "-o", dir.resolve("solution").toString())
                .directory(dir.toFile()).redirectErrorStream(true).start();
        if (!gcc.waitFor(10, TimeUnit.SECONDS) || gcc.exitValue() != 0) {
            return ExecutionResult.compilationError(readStream(gcc.getInputStream()));
        }

        // Запуск
        Process run = new ProcessBuilder(dir.resolve("solution").toString())
                .directory(dir.toFile()).start();
        if (input != null) {
            run.getOutputStream().write(input.getBytes());
            run.getOutputStream().close();
        }
        if (!run.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            run.destroyForcibly();
            return ExecutionResult.timeout();
        }
        String output = readStream(run.getInputStream());
        return new ExecutionResult(run.exitValue() == 0, output, null);
    }
}
```

Требует установленный GCC на сервере. Менее безопасно, но подходит для курсовой.

**Вариант Б — Docker (безопасно):**

Использовать `docker run --rm --network=none --memory=64m --cpus=0.5 gcc:alpine sh -c '...'`
через ProcessBuilder. Полная изоляция.

**Что менять в SubmissionService:**

```java
// Вместо checkSolution():
List<TestCase> tests = testCaseRepo.findByTaskIdOrderByOrderIndexAsc(task.getId());
int passed = 0;
for (TestCase tc : tests) {
    ExecutionResult result = codeExecutionService.execute(code, tc.getInput(), 5000);
    if (result.isSuccess() && result.getOutput().trim().equals(tc.getExpectedOutput().trim())) {
        passed++;
    }
}
```

**Зачем:** главный критерий — «качество проверки решений задач».

---

### 4.3 🟡 Видимость тестов для студента [S]

**Сейчас:** студент не видит тестовые данные.
**Надо:** sample-тесты (с `isSample = true`) видны студенту до отправки решения.
Скрытые тесты — не видны. После отправки — показать, на каком тесте упало.

**Что делать:**

1. В `TaskDto` — добавить `List<TestCaseDto> sampleTests`.
2. В `TaskService.toDto()` — отфильтровать: `tests.stream().filter(TestCase::isSample)`.
3. В `SubmissionResponse` — добавить `List<TestResultDto> testResults`:
   ```java
   public record TestResultDto(int testNumber, boolean passed, String input, String expected, String actual) {}
   ```
   Для sample-тестов показывать input/expected/actual. Для скрытых — только passed/failed.
4. Фронтенд: на странице задачи — блок «Примеры» с input/output. После сабмита — таблица
   результатов по тестам.

---

## Фаза 5 — Технические улучшения

---

### 5.1 🟡 Пагинация API [M]

**Сейчас:** все списки отдаются целиком.
**Надо:** `GET /api/lessons?page=0&size=10` возвращает `Page<LessonDto>`.

**Что делать:** Spring Data уже поддерживает — просто заменить `List<>` на `Page<>`:

```java
// Repository:
Page<Lesson> findAllByOrderByOrderIndexAsc(Pageable pageable);

// Controller:
@GetMapping
public ResponseEntity<Page<LessonDto>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(lessonService.findAll(PageRequest.of(page, size)));
}
```

---

### 5.2 🟡 Unit-тесты [L]

**Сейчас:** тестов нет.
**Надо:** хотя бы базовые тесты для ключевых сервисов.

**Что тестировать:**

| Класс | Что тестировать | Тип |
|-------|----------------|-----|
| `GamificationService` | Формула уровня, начисление XP, выдача достижений | Unit |
| `AuthService` | Регистрация, логин, дублирование username | Unit |
| `SubmissionService` | Верный/неверный ответ, повторное решение не даёт XP | Unit |
| `JwtService` | Генерация и валидация токена | Unit |
| Контроллеры | Статус-коды, авторизация | Integration |

**Структура файлов:**

```
src/test/java/org/example/
├── service/
│   ├── GamificationServiceTest.java
│   ├── AuthServiceTest.java
│   └── SubmissionServiceTest.java
└── controller/
    └── AuthControllerTest.java
```

**Пример теста:**

```java
@SpringBootTest
class GamificationServiceTest {
    @Autowired GamificationService svc;

    @Test
    void levelCalculation() {
        assertEquals(1, svc.calculateLevel(0));
        assertEquals(1, svc.calculateLevel(99));
        assertEquals(2, svc.calculateLevel(100));
        assertEquals(3, svc.calculateLevel(400));
    }
}
```

**Зачем:** «Тестирование и отладка» — отдельный этап в ТЗ. Наличие тестов — плюс на защите.

---

### 5.3 🟡 Логирование [S]

**Сейчас:** логирование по умолчанию (Spring Boot).
**Надо:** осмысленные логи в сервисах.

```java
// В сервисе:
private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

// В методе:
log.info("User {} submitted solution for task {}", username, taskId);
log.info("Result: {} | XP earned: {}", status, xpEarned);
log.warn("Failed login attempt for username: {}", username);
```

---

### 5.4 🟢 Swagger / OpenAPI документация [S]

Автоматическая документация API.

1. Добавить зависимость в `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.5.0</version>
   </dependency>
   ```
2. Разрешить в `SecurityConfig`: `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()`.
3. Доступно по адресу: `http://localhost:8080/swagger-ui.html`.

---

### 5.5 🟢 Переключение на PostgreSQL в проде [S]

Убедиться, что миграции работают и на H2, и на PostgreSQL. Для этого —
проверить синтаксис (`AUTO_INCREMENT` → `GENERATED ALWAYS AS IDENTITY`, `CLOB` → `TEXT`)
или сделать два набора миграций.

---

## Фаза 6 — Расширенный функционал (вау-фактор)

---

### 6.1 🟢 AI-ассистент по коду [L]

Интеграция с OpenAI / Ollama для подсказок студенту.

**Вариант А — Серверный (рекомендуется для курсовой):**

1. `AiService` — вызывает OpenAI API (или локальный Ollama) с промптом:
   «Студент решает задачу: {описание}. Его код: {код}. Ошибка: {output}. Дай подсказку, не давай готовый ответ.»
2. `POST /api/ai/hint` — body: `{ taskId, code }` → response: `{ hint }`.
3. Фронтенд: кнопка «🤖 Спросить ассистента» рядом с редактором кода.

**Вариант Б — Заготовка (без реального AI):**

Генерировать «умные» подсказки на основе анализа кода:
- Нет `#include <stdio.h>` → «Не забудьте подключить стандартную библиотеку ввода-вывода»
- Нет `return 0` → «Функция main должна возвращать 0»
- Нет `;` в конце строк → «Проверьте точки с запятой»

---

### 6.2 🟢 Модуль «Теория» с отдельными статьями [M]

Помимо теории внутри урока — отдельный раздел «Библиотека» со справочными статьями.

1. Таблица `articles` (id, title, content, category, order_index, author_id).
2. Категории: «Справочник», «Лайфхаки», «Частые ошибки».
3. Не привязаны к урокам — студент может читать в любой момент.
4. Фронтенд: новый view «📚 Библиотека» с карточками статей, поиском по названию.

---

### 6.3 🟢 Система уведомлений [M]

1. Таблица `notifications` (id, user_id, title, message, is_read, created_at).
2. Уведомления генерируются при: получении достижения, создании нового челленджа,
   публикации нового урока.
3. `GET /api/notifications` — непрочитанные.
4. `PUT /api/notifications/{id}/read` — пометить прочитанным.
5. Фронтенд: колокольчик в топбаре с бейджем количества непрочитанных.

---

### 6.4 🟢 Тёмная/светлая тема [S]

Кнопка переключения в сайдбаре. CSS-переменные уже используются — достаточно определить
второй набор `--bg`, `--surface`, `--text` и переключать класс `body.light`.

---

### 6.5 🟢 Экспорт прогресса в PDF [M]

Преподаватель может выгрузить отчёт по студенту: имя, уроки, задачи, статус, XP.
Использовать библиотеку `iText` или `Apache PDFBox`.

---

## Сводная таблица приоритетов

| # | Задача | Приоритет | Размер | Фаза |
|---|--------|-----------|--------|------|
| 1.1 | Множественные тест-кейсы | 🔴 | L | 1 |
| 1.2 | Просмотр прогресса студентов | 🔴 | L | 1 |
| 1.3 | Челленджи | 🔴 | XL | 1 |
| 4.1 | Наполнение контентом (20+ задач) | 🔴 | L | 4 |
| 2.1 | Личный кабинет студента | 🔴 | L | 2 |
| 1.4 | Лидерборд | 🟠 | M | 1 |
| 1.5 | Streak | 🟠 | M | 1 |
| 1.6 | Задача дня | 🟠 | M | 1 |
| 2.2 | История отправок | 🟠 | S | 2 |
| 2.3 | Полный CRUD уроков/задач | 🟠 | M | 2 |
| 3.1 | Расширенная система достижений | 🟠 | M | 3 |
| 4.2 | Компиляция C-кода (Docker) | 🟠 | XL | 4 |
| 4.3 | Видимость тестов | 🟡 | S | 4 |
| 2.4 | Подсветка синтаксиса | 🟡 | M | 2 |
| 2.5 | Markdown в теории | 🟡 | S | 2 |
| 5.1 | Пагинация | 🟡 | M | 5 |
| 5.2 | Unit-тесты | 🟡 | L | 5 |
| 5.3 | Логирование | 🟡 | S | 5 |
| 3.2 | Система титулов | 🟡 | S | 3 |
| 3.3 | Анимация Level Up | 🟢 | S | 3 |
| 3.4 | Звуковые эффекты | 🟢 | S | 3 |
| 5.4 | Swagger | 🟢 | S | 5 |
| 5.5 | PostgreSQL в проде | 🟢 | S | 5 |
| 6.1 | AI-ассистент | 🟢 | L | 6 |
| 6.2 | Библиотека статей | 🟢 | M | 6 |
| 6.3 | Уведомления | 🟢 | M | 6 |
| 6.4 | Тёмная/светлая тема | 🟢 | S | 6 |
| 6.5 | Экспорт в PDF | 🟢 | M | 6 |

---

## Рекомендуемый порядок работы

```
Неделя 1:  1.1 (тест-кейсы) + 2.2 (история отправок) + 4.1 (контент)
Неделя 2:  1.2 (прогресс учителя) + 2.3 (CRUD) + 1.4 (лидерборд)
Неделя 3:  1.5 (streak) + 1.6 (задача дня) + 3.1 (достижения) + 3.2 (титулы)
Неделя 4:  1.3 (челленджи) + 2.1 (личный кабинет)
Неделя 5:  4.2 (компиляция) или 2.4 (подсветка) + 5.2 (тесты) + 2.5 (markdown)
Неделя 6:  Полировка + бонусные фичи (6.x) + подготовка к защите
```

**Минимум для «зачёт»:** фазы 1 + 2 + наполнение контентом.
**На «отлично»:** фазы 1–4 + хотя бы пару задач из фазы 5.
**Вау-эффект:** что-то из фазы 6.

---

## Критерии из ТЗ → покрытие задачами

| Критерий из документа | Какие задачи покрывают |
|----------------------|----------------------|
| Работа с базой данных | Уже есть (JPA/Hibernate) + 1.1, 1.3 расширяют |
| Управление доступом | Уже есть (JWT + роли) + 1.2 усиливает |
| Функциональность | 1.1–1.6, 2.1–2.3, 4.1 |
| Качество проверки решений | 1.1 (тесты), 4.2 (компиляция), 4.3 (видимость) |
| Элементы геймификации | 1.3–1.6, 3.1–3.4 |
| Документация | Уже есть (README + DEVELOPMENT.md) + 5.4 (Swagger) |

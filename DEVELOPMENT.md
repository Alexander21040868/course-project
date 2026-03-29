# CodeQuest — Документация разработчика

> Подробное руководство по архитектуре, устройству каждого слоя и пошаговые инструкции
> по расширению функционала приложения.

---

## Оглавление

1. [Общая архитектура](#1-общая-архитектура)
2. [Структура проекта (каждый файл)](#2-структура-проекта)
3. [Как работает запуск приложения](#3-как-работает-запуск-приложения)
4. [Слой Entity — сущности и база данных](#4-слой-entity)
5. [Слой Repository — доступ к данным](#5-слой-repository)
6. [Слой Service — бизнес-логика](#6-слой-service)
7. [Слой Controller — REST API](#7-слой-controller)
8. [DTO — объекты передачи данных](#8-dto)
9. [Безопасность: JWT + Spring Security](#9-безопасность)
10. [Система геймификации](#10-система-геймификации)
11. [База данных и миграции Flyway](#11-база-данных-и-миграции)
12. [Фронтенд: как устроен UI](#12-фронтенд)
13. [Пошаговые рецепты расширения](#13-рецепты-расширения)
    - 13.1 [Добавить новую сущность](#131-добавить-новую-сущность)
    - 13.2 [Добавить новый API-эндпоинт](#132-добавить-новый-api-эндпоинт)
    - 13.3 [Добавить новое достижение](#133-добавить-новое-достижение)
    - 13.4 [Добавить новую страницу на фронтенде](#134-добавить-новую-страницу-на-фронтенде)
    - 13.5 [Добавить лидерборд](#135-добавить-лидерборд)
    - 13.6 [Добавить streak (серию дней)](#136-добавить-streak)
    - 13.7 [Добавить челленджи / соревнования](#137-добавить-челленджи)
14. [Переключение на PostgreSQL](#14-переключение-на-postgresql)
15. [Типичные ошибки и отладка](#15-типичные-ошибки)

---

## 1. Общая архитектура

Приложение построено по **трёхслойной архитектуре** (three-tier):

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND (браузер)                       │
│            HTML + CSS + JavaScript (Fetch API)               │
│         index.html (логин) → app.html (основное)            │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTP / JSON
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   CONTROLLER (REST API)                      │
│  AuthController · LessonController · TaskController          │
│  SubmissionController · ProfileController · AdminController  │
└──────────────────────────┬──────────────────────────────────┘
                           │  вызовы методов
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    SERVICE (бизнес-логика)                    │
│  AuthService · LessonService · TaskService                   │
│  SubmissionService · GamificationService · JwtService        │
└──────────────────────────┬──────────────────────────────────┘
                           │  Spring Data JPA
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   REPOSITORY (доступ к БД)                    │
│  UserRepository · LessonRepository · TaskRepository          │
│  SubmissionRepository · AchievementRepository                │
│  UserAchievementRepository                                   │
└──────────────────────────┬──────────────────────────────────┘
                           │  SQL (Hibernate генерирует)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     DATABASE (H2 / PostgreSQL)               │
│  Таблицы: users, lessons, tasks, submissions,                │
│           achievements, user_achievements                    │
└─────────────────────────────────────────────────────────────┘
```

**Принцип:** каждый слой знает только о слое ниже. Controller вызывает Service,
Service вызывает Repository, Repository работает с базой. Никогда наоборот.

### Поток данных на примере «студент отправляет решение»

```
1. Браузер → POST /api/submissions  { taskId: 1, code: "..." }
2. JwtAuthFilter проверяет токен из заголовка Authorization
3. SubmissionController получает запрос, извлекает username из Principal
4. SubmissionController вызывает SubmissionService.submit(req, username)
5. SubmissionService:
   a. Находит User и Task через репозитории
   b. Проверяет код (метод checkSolution)
   c. Сохраняет Submission в БД
   d. Если верно — вызывает GamificationService.addXp() и checkAndGrantAchievements()
   e. Возвращает SubmissionResponse
6. Controller отдаёт JSON-ответ → браузер показывает результат
```

---

## 2. Структура проекта

```
untitled1/
│
├── pom.xml                          ← Maven: зависимости и сборка
├── Dockerfile                       ← Docker-образ приложения
├── docker-compose.yml               ← Docker Compose (app + PostgreSQL)
├── README.md                        ← Краткое описание проекта
├── DEVELOPMENT.md                   ← Эта документация
│
└── src/main/
    ├── java/org/example/
    │   │
    │   ├── CodeQuestApplication.java        ← Точка входа (main)
    │   │
    │   ├── config/                          ← КОНФИГУРАЦИЯ
    │   │   ├── SecurityConfig.java          ← Настройка Spring Security
    │   │   ├── JwtAuthFilter.java           ← Фильтр проверки JWT-токенов
    │   │   └── DataInitializer.java         ← Заполнение БД демо-данными
    │   │
    │   ├── entity/                          ← JPA-СУЩНОСТИ (таблицы БД)
    │   │   ├── User.java                    ← Пользователь
    │   │   ├── Role.java                    ← Enum: STUDENT, TEACHER
    │   │   ├── Lesson.java                  ← Урок
    │   │   ├── Task.java                    ← Задача
    │   │   ├── Difficulty.java              ← Enum: EASY, MEDIUM, HARD
    │   │   ├── Submission.java              ← Отправленное решение
    │   │   ├── SubmissionStatus.java        ← Enum: PENDING, CORRECT, WRONG
    │   │   ├── Achievement.java             ← Определение достижения
    │   │   └── UserAchievement.java         ← Связь пользователь ↔ достижение
    │   │
    │   ├── repository/                      ← РЕПОЗИТОРИИ (доступ к данным)
    │   │   ├── UserRepository.java
    │   │   ├── LessonRepository.java
    │   │   ├── TaskRepository.java
    │   │   ├── SubmissionRepository.java
    │   │   ├── AchievementRepository.java
    │   │   └── UserAchievementRepository.java
    │   │
    │   ├── service/                         ← СЕРВИСЫ (бизнес-логика)
    │   │   ├── AuthService.java             ← Регистрация и вход
    │   │   ├── JwtService.java              ← Генерация и проверка JWT
    │   │   ├── LessonService.java           ← CRUD уроков
    │   │   ├── TaskService.java             ← CRUD задач
    │   │   ├── SubmissionService.java       ← Приём и проверка решений
    │   │   └── GamificationService.java     ← XP, уровни, достижения
    │   │
    │   ├── controller/                      ← REST-КОНТРОЛЛЕРЫ (API)
    │   │   ├── AuthController.java          ← /api/auth/**
    │   │   ├── LessonController.java        ← /api/lessons/**
    │   │   ├── TaskController.java          ← /api/tasks/**
    │   │   ├── SubmissionController.java    ← /api/submissions
    │   │   ├── ProfileController.java       ← /api/profile
    │   │   └── AdminController.java         ← /api/admin/** (только TEACHER)
    │   │
    │   ├── dto/                             ← DTO (объекты для API)
    │   │   ├── AuthRequest.java
    │   │   ├── AuthResponse.java
    │   │   ├── LessonDto.java
    │   │   ├── LessonCreateRequest.java
    │   │   ├── TaskDto.java
    │   │   ├── TaskCreateRequest.java
    │   │   ├── SubmissionRequest.java
    │   │   ├── SubmissionResponse.java
    │   │   ├── ProfileDto.java
    │   │   └── AchievementDto.java
    │   │
    │   └── exception/                       ← ОБРАБОТКА ОШИБОК
    │       └── GlobalExceptionHandler.java
    │
    └── resources/
        ├── application.yml                  ← Конфигурация Spring Boot
        ├── db/migration/
        │   └── V1__init.sql                 ← Flyway: создание таблиц
        └── static/                          ← ФРОНТЕНД
            ├── index.html                   ← Страница логина
            ├── app.html                     ← Основное приложение (SPA)
            ├── css/style.css                ← Все стили
            └── js/
                ├── api.js                   ← Обёртка для Fetch API
                └── app.js                   ← Логика интерфейса
```

---

## 3. Как работает запуск приложения

### Последовательность при `mvn spring-boot:run`:

```
1. Maven компилирует Java-файлы, копирует resources в target/classes
2. Spring Boot запускает CodeQuestApplication.main()
3. Spring сканирует пакет org.example и находит все @Component, @Service,
   @Repository, @Controller, @Configuration — создаёт бины
4. Flyway проверяет таблицу flyway_schema_history в БД
   → если миграция V1__init.sql ещё не выполнена — выполняет
5. Hibernate проверяет соответствие Entity-классов и таблиц (ddl-auto: validate)
6. SecurityConfig настраивает цепочку фильтров безопасности
7. DataInitializer.run() проверяет — есть ли пользователи в БД?
   → если нет — создаёт демо-данных (teacher, student, уроки, задачи)
8. Tomcat запускается на порту 8080
9. Приложение готово: http://localhost:8080
```

### Что происходит при HTTP-запросе:

```
HTTP запрос
   │
   ▼
Tomcat (встроенный сервер)
   │
   ▼
Spring Security Filter Chain:
   ├── JwtAuthFilter — проверяет Bearer-токен
   ├── Если токен валиден → устанавливает SecurityContext (username + role)
   └── AuthorizationFilter — проверяет, разрешён ли доступ к URL
   │
   ▼
DispatcherServlet → находит нужный @RestController по URL
   │
   ▼
Controller метод → вызывает Service → Repository → БД
   │
   ▼
Результат сериализуется в JSON → HTTP ответ
```

---

## 4. Слой Entity

Entity-классы — это Java-классы, которые **отображаются на таблицы в базе данных** через JPA-аннотации.

### Основные аннотации

| Аннотация | Что делает | Пример |
|-----------|-----------|--------|
| `@Entity` | Помечает класс как JPA-сущность | `@Entity public class User` |
| `@Table(name = "users")` | Указывает имя таблицы | — |
| `@Id` | Первичный ключ | `private Long id` |
| `@GeneratedValue(strategy = IDENTITY)` | Автоинкремент | — |
| `@Column(nullable, unique, length)` | Настройки столбца | `@Column(nullable = false, unique = true)` |
| `@Enumerated(EnumType.STRING)` | Хранить enum как строку | `private Role role` |
| `@ManyToOne(fetch = LAZY)` | Связь «много к одному» | `Task → Lesson` |
| `@JoinColumn(name = "...")` | Внешний ключ | `@JoinColumn(name = "lesson_id")` |
| `@Lob` | Длинный текст (CLOB) | `private String content` |

### Модель данных (ER-диаграмма текстом)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │   lessons    │       │    tasks     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │──┐    │ id (PK)      │──┐    │ id (PK)      │
│ username     │  │    │ title        │  │    │ lesson_id(FK)│──→ lessons.id
│ email        │  │    │ description  │  │    │ title        │
│ password     │  │    │ content      │  │    │ description  │
│ role         │  │    │ order_index  │  │    │ difficulty   │
│ xp           │  └──→ │ author_id(FK)│  │    │ xp_reward    │
│ level        │       │ created_at   │  │    │ template_code│
│ created_at   │       └──────────────┘  │    │ expected_out │
└──────┬───────┘                         │    │ hints        │
       │                                 │    │ order_index  │
       │       ┌──────────────┐          │    └──────┬───────┘
       │       │ submissions  │          │           │
       │       ├──────────────┤          │           │
       └─────→ │ user_id (FK) │          │           │
               │ task_id (FK) │──────────┘───────────┘
               │ code         │
               │ status       │
               │ output       │
               │ submitted_at │
               └──────────────┘

┌──────────────┐       ┌───────────────────┐
│ achievements │       │ user_achievements │
├──────────────┤       ├───────────────────┤
│ id (PK)      │──┐    │ id (PK)           │
│ code         │  └──→ │ achievement_id(FK)│
│ name         │       │ user_id (FK)      │──→ users.id
│ description  │       │ earned_at         │
│ icon         │       └───────────────────┘
│ xp_reward    │
│ threshold    │
└──────────────┘
```

### Как устроена связь между сущностями

**Пример: Task принадлежит Lesson (ManyToOne)**

```java
// В Task.java:
@ManyToOne(fetch = FetchType.LAZY)     // «много задач → один урок»
@JoinColumn(name = "lesson_id",        // имя столбца-FK в таблице tasks
            nullable = false)
private Lesson lesson;
```

- `FetchType.LAZY` — урок загружается из БД **только когда** к нему обращаются
  (экономит производительность)
- `nullable = false` — у каждой задачи обязательно есть урок

**Пример: UserAchievement связывает User и Achievement**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;

@ManyToOne(fetch = FetchType.EAGER)    // EAGER — загружаем достижение сразу
@JoinColumn(name = "achievement_id", nullable = false)
private Achievement achievement;
```

---

## 5. Слой Repository

Repository — это **интерфейсы**, которые наследуют `JpaRepository<Entity, IdType>`.
Spring Data автоматически создаёт реализацию на основе имён методов.

### Как Spring Data генерирует SQL из имени метода

| Метод в интерфейсе | Сгенерированный SQL |
|---------------------|---------------------|
| `findByUsername(String u)` | `SELECT * FROM users WHERE username = ?` |
| `findAllByOrderByOrderIndexAsc()` | `SELECT * FROM lessons ORDER BY order_index ASC` |
| `findByLessonIdOrderByOrderIndexAsc(Long id)` | `SELECT * FROM tasks WHERE lesson_id = ? ORDER BY order_index ASC` |
| `existsByUsername(String u)` | `SELECT COUNT(*) > 0 FROM users WHERE username = ?` |
| `countByUserIdAndStatus(Long uid, Status s)` | `SELECT COUNT(*) FROM submissions WHERE user_id = ? AND status = ?` |
| `existsByUserIdAndTaskIdAndStatus(...)` | `SELECT COUNT(*) > 0 FROM submissions WHERE user_id = ? AND task_id = ? AND status = ?` |

### Правила именования

```
find  By  Username  And  Email   OrderBy  CreatedAt  Desc
│     │   │         │    │       │        │          │
│     │   │         │    │       │        │          └─ направление сортировки
│     │   │         │    │       │        └─ поле сортировки
│     │   │         │    │       └─ ключевое слово сортировки
│     │   │         │    └─ второе поле условия
│     │   │         └─ логический оператор (And / Or)
│     │   └─ первое поле условия (имя поля в Entity!)
│     └─ начало условия
└─ тип операции (find / exists / count / delete)
```

### Доступные «ключевые слова»

| Ключевое слово | Пример | SQL-эквивалент |
|----------------|--------|----------------|
| `And` | `findByNameAndAge` | `WHERE name = ? AND age = ?` |
| `Or` | `findByNameOrAge` | `WHERE name = ? OR age = ?` |
| `Between` | `findByAgeBetween` | `WHERE age BETWEEN ? AND ?` |
| `LessThan` | `findByAgeLessThan` | `WHERE age < ?` |
| `GreaterThan` | `findByAgeGreaterThan` | `WHERE age > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE '%?%'` |
| `In` | `findByIdIn(List)` | `WHERE id IN (...)` |
| `IsNull` | `findByEmailIsNull` | `WHERE email IS NULL` |
| `OrderBy...Asc/Desc` | `findByRoleOrderByXpDesc` | `ORDER BY xp DESC` |
| `Top` / `First` | `findTop5ByOrderByXpDesc` | `LIMIT 5` |
| `Distinct` | `countDistinctTaskBy...` | `COUNT(DISTINCT ...)` |

### Пример: полный UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // JpaRepository уже даёт бесплатно:
    //   save(entity)       — INSERT или UPDATE
    //   findById(id)       — SELECT ... WHERE id = ?
    //   findAll()          — SELECT *
    //   deleteById(id)     — DELETE ... WHERE id = ?
    //   count()            — SELECT COUNT(*)
    //   existsById(id)     — SELECT COUNT(*) > 0 ...

    // Наши кастомные методы:
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

---

## 6. Слой Service

Сервисы содержат **бизнес-логику** — всё, что сложнее «достань из базы и отдай».

### Паттерн сервиса

```java
@Service                              // Spring создаст бин
@Transactional(readOnly = true)       // по умолчанию все методы — только чтение
public class LessonService {

    private final LessonRepository lessonRepo;   // внедрение через конструктор
    private final TaskRepository taskRepo;

    public LessonService(LessonRepository lessonRepo, TaskRepository taskRepo, ...) {
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
    }

    // Метод чтения — readOnly = true (из класса)
    public List<LessonDto> findAll() {
        return lessonRepo.findAllByOrderByOrderIndexAsc().stream()
                .map(this::toDto)         // конвертируем Entity → DTO
                .toList();
    }

    @Transactional                        // Метод записи — переопределяем readOnly
    public LessonDto create(LessonCreateRequest req, String authorUsername) {
        // бизнес-логика
        User author = userRepo.findByUsername(authorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Не найден"));
        Lesson lesson = new Lesson();
        lesson.setTitle(req.title());
        // ...
        return toDto(lessonRepo.save(lesson));   // save() → INSERT
    }

    // Приватный маппер Entity → DTO
    private LessonDto toDto(Lesson l) {
        return new LessonDto(l.getId(), l.getTitle(), ...);
    }
}
```

### Ключевые правила

1. **Сервис НИКОГДА не возвращает Entity наружу** — только DTO
2. **Все обращения к Repository — через сервис**, контроллер не вызывает репозиторий
   напрямую (исключение: `ProfileController` — упрощение для студенческого проекта)
3. **`@Transactional`** гарантирует, что все операции в методе выполнятся атомарно
   (либо все, либо ни одна)
4. **Исключения** типа `IllegalArgumentException` — ловит `GlobalExceptionHandler`
   и конвертирует в HTTP 400

### Как устроен SubmissionService (самый сложный)

```java
@Transactional
public SubmissionResponse submit(SubmissionRequest req, String username) {
    // 1. Найти пользователя и задачу
    User user = userRepo.findByUsername(username).orElseThrow(...);
    Task task = taskRepo.findById(req.taskId()).orElseThrow(...);

    // 2. Проверить, не решена ли задача ранее (чтобы не давать XP дважды)
    boolean alreadySolved = submissionRepo.existsByUserIdAndTaskIdAndStatus(
            user.getId(), task.getId(), SubmissionStatus.CORRECT);

    // 3. Создать запись Submission
    Submission sub = new Submission();
    sub.setUser(user);
    sub.setTask(task);
    sub.setCode(req.code());

    // 4. Проверить решение
    String result = checkSolution(req.code(), task);
    boolean correct = "OK".equals(result);
    sub.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.WRONG);
    sub.setOutput(correct ? "Верно!" : result);
    submissionRepo.save(sub);

    // 5. Начислить XP и проверить достижения (только при первом решении)
    int xpEarned = 0;
    List<AchievementDto> newAchievements = new ArrayList<>();
    if (correct && !alreadySolved) {
        xpEarned = gamificationService.addXp(user, task.getXpReward());
        newAchievements = gamificationService.checkAndGrantAchievements(user);
    }

    // 6. Вернуть DTO
    return new SubmissionResponse(sub.getId(), sub.getStatus().name(), ...);
}
```

### Логика проверки решений (`checkSolution`)

Текущая реализация — **упрощённая** (без реальной компиляции C-кода):

```
1. Если у задачи нет expectedOutput → любой непустой код = OK
2. Если код пустой → ошибка
3. Если нет функции main → ошибка
4. containsExpectedLogic() — проверяет:
   a. Содержит ли код ожидаемый вывод как подстроку
   b. Есть ли printf/puts/cout и ≥50% ключевых слов из ожидаемого вывода
```

**Как улучшить:** реальная компиляция и запуск C-кода в Docker-контейнере (песочница).
Подробнее — в разделе рецептов.

---

## 7. Слой Controller

Контроллеры принимают HTTP-запросы, вызывают сервисы и возвращают JSON.

### Аннотации контроллера

| Аннотация | Что делает |
|-----------|-----------|
| `@RestController` | Помечает класс как REST API контроллер (все методы возвращают JSON) |
| `@RequestMapping("/api/...")` | Базовый путь для всех эндпоинтов класса |
| `@GetMapping("/{id}")` | Обработка GET-запроса |
| `@PostMapping` | Обработка POST-запроса |
| `@DeleteMapping("/{id}")` | Обработка DELETE-запроса |
| `@PathVariable Long id` | Извлечение значения из URL (`/api/tasks/5` → id=5) |
| `@RequestBody` | Десериализация JSON тела запроса в Java-объект |
| `@Valid` | Включение проверки (аннотации `@NotBlank`, `@NotNull` из DTO) |
| `Principal principal` | Spring автоматически подставляет текущего пользователя |

### Шаблон контроллера

```java
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    // Конструктор — Spring внедряет зависимость автоматически
    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping                  // GET /api/lessons
    public ResponseEntity<List<LessonDto>> getAll() {
        return ResponseEntity.ok(lessonService.findAll());
    }

    @GetMapping("/{id}")         // GET /api/lessons/3
    public ResponseEntity<LessonDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.findById(id));
    }
}
```

### Карта всех эндпоинтов

```
ПУБЛИЧНЫЕ (без токена):
  POST /api/auth/register    ← регистрация
  POST /api/auth/login       ← вход
  GET  /                     ← index.html
  GET  /app.html             ← основное приложение
  GET  /css/**               ← стили
  GET  /js/**                ← скрипты
  GET  /h2/**                ← H2 Console (для отладки)

АУТЕНТИФИЦИРОВАННЫЕ (нужен Bearer-токен):
  GET  /api/profile          ← профиль (XP, уровень, достижения)
  GET  /api/lessons          ← список уроков
  GET  /api/lessons/{id}     ← один урок
  GET  /api/tasks/lesson/{id}← задачи урока
  GET  /api/tasks/{id}       ← одна задача
  POST /api/submissions      ← отправить решение

ТОЛЬКО ДЛЯ TEACHER:
  POST   /api/admin/lessons  ← создать урок
  DELETE /api/admin/lessons/{id} ← удалить урок
  POST   /api/admin/tasks    ← создать задачу
  DELETE /api/admin/tasks/{id}   ← удалить задачу
```

### Как получить текущего пользователя

В методе контроллера добавьте `Principal principal`:

```java
@GetMapping
public ResponseEntity<ProfileDto> getProfile(Principal principal) {
    String username = principal.getName();  // имя из JWT-токена
    // ... используйте username для поиска User
}
```

### Как ограничить доступ к эндпоинту

В `SecurityConfig.java`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()           // всем
    .requestMatchers("/api/admin/**").hasRole("TEACHER")   // только TEACHER
    .anyRequest().authenticated()                          // остальное — авторизован
)
```

---

## 8. DTO

DTO (Data Transfer Object) — **промежуточные объекты** между Entity и JSON.

### Зачем нужны DTO?

1. **Безопасность** — не отдаём пароль пользователя в JSON
2. **Гибкость** — JSON может отличаться от структуры БД
3. **Декоратирование** — добавляем вычисляемые поля (taskCount, solved)

### Java Records (Java 17+)

DTO в проекте реализованы как `record` — компактный синтаксис:

```java
// Это:
public record LessonDto(
    Long id,
    String title,
    String description,
    String content,
    int orderIndex,
    String authorName,    // не ID автора, а его имя!
    int taskCount         // вычисляемое поле
) {}

// Эквивалентно полному классу с:
//   - private final полями
//   - конструктором
//   - геттерами (id(), title(), ... — без get-префикса)
//   - equals(), hashCode(), toString()
```

### Где DTO используются

| DTO | Где используется | Направление |
|-----|-----------------|-------------|
| `AuthRequest` | `/api/auth/login`, `/register` | Запрос → сервер |
| `AuthResponse` | `/api/auth/login`, `/register` | Сервер → клиент |
| `LessonDto` | `/api/lessons` | Сервер → клиент |
| `LessonCreateRequest` | `/api/admin/lessons` | Запрос → сервер |
| `TaskDto` | `/api/tasks` | Сервер → клиент |
| `TaskCreateRequest` | `/api/admin/tasks` | Запрос → сервер |
| `SubmissionRequest` | `/api/submissions` | Запрос → сервер |
| `SubmissionResponse` | `/api/submissions` | Сервер → клиент |
| `ProfileDto` | `/api/profile` | Сервер → клиент |
| `AchievementDto` | внутри ProfileDto, SubmissionResponse | Сервер → клиент |

### Валидация

```java
public record SubmissionRequest(
    @NotNull Long taskId,      // не может быть null
    @NotBlank String code      // не может быть null, пустым или пробелами
) {}
```

Работает в паре с `@Valid` в контроллере:
```java
public ResponseEntity<...> submit(@Valid @RequestBody SubmissionRequest req, ...)
```

---

## 9. Безопасность

### Как работает JWT-аутентификация (пошагово)

```
  ┌─ РЕГИСТРАЦИЯ / ВХОД ─────────────────────────────────────┐
  │                                                           │
  │  POST /api/auth/login { username, password }              │
  │         │                                                 │
  │         ▼                                                 │
  │  AuthService.login()                                      │
  │    1. Ищет пользователя по username                       │
  │    2. Сравнивает пароль через BCrypt                      │
  │    3. Генерирует JWT через JwtService.generateToken()     │
  │         │                                                 │
  │         ▼                                                 │
  │  Ответ: { token: "eyJhbG...", username, role }            │
  │  Браузер сохраняет токен в localStorage                   │
  └───────────────────────────────────────────────────────────┘

  ┌─ ПОСЛЕДУЮЩИЕ ЗАПРОСЫ ─────────────────────────────────────┐
  │                                                           │
  │  GET /api/lessons                                         │
  │  Headers: Authorization: Bearer eyJhbG...                 │
  │         │                                                 │
  │         ▼                                                 │
  │  JwtAuthFilter.doFilterInternal()                         │
  │    1. Извлекает токен из заголовка                        │
  │    2. Вызывает JwtService.isValid(token)                  │
  │    3. Извлекает username и role из токена                  │
  │    4. Создаёт UsernamePasswordAuthenticationToken          │
  │    5. Устанавливает в SecurityContextHolder               │
  │         │                                                 │
  │         ▼                                                 │
  │  AuthorizationFilter проверяет права доступа              │
  │  Controller получает Principal с username                  │
  └───────────────────────────────────────────────────────────┘
```

### Структура JWT-токена

JWT = три части, разделённые точками: `header.payload.signature`

```json
// Header (алгоритм)
{ "alg": "HS384" }

// Payload (данные)
{
  "sub": "student",          // username
  "role": "STUDENT",         // роль
  "iat": 1711000000,         // issued at (когда создан)
  "exp": 1711086400          // expiration (когда истечёт, +24h)
}

// Signature — подпись, гарантирует что токен не подделан
HMAC-SHA384(header + "." + payload, SECRET_KEY)
```

### Настройка секретного ключа

В `application.yml`:

```yaml
app:
  jwt:
    secret: Y29kZXF1ZXN0LXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLTIwMjQ=
    expiration-ms: 86400000  # 24 часа в миллисекундах
```

> **Важно:** в продакшене замените secret на случайную строку в Base64 (≥ 32 байт).

### BCrypt — хеширование паролей

Пароли **никогда** не хранятся в открытом виде. `PasswordEncoder`:
- При регистрации: `encoder.encode("student")` → `$2a$10$Xq...` (хеш)
- При входе: `encoder.matches("student", хеш_из_БД)` → `true/false`

---

## 10. Система геймификации

### XP и уровни

**Начисление XP:**
- За правильное решение задачи: +`task.xpReward` XP (15–50 в зависимости от сложности)
- За получение достижения: +`achievement.xpReward` XP
- XP начисляется **только при первом** правильном решении задачи

**Формула уровня:**

```
level = floor( sqrt(xp / 100) ) + 1
```

| XP | Уровень | Следующий уровень при |
|----|---------|----------------------|
| 0–99 | 1 | 100 XP |
| 100–399 | 2 | 400 XP |
| 400–899 | 3 | 900 XP |
| 900–1599 | 4 | 1600 XP |
| 1600–2499 | 5 | 2500 XP |

### Достижения

Определены в таблице `achievements` (создаются в миграции V1):

| Код | Название | Условие | Бонус XP |
|-----|---------|---------|----------|
| `FIRST_BLOOD` | Первая кровь | 1 задача решена | +50 |
| `SOLVER_10` | Подмастерье | 10 задач решено | +100 |
| `SOLVER_50` | Мастер кода | 50 задач решено | +300 |
| `PERFECT_5` | Без единой ошибки | 5 задач с первой попытки | +150 |
| `SPEEDRUN` | Спидраннер | Задача за 60 сек | +75 |
| `ALL_EASY` | Разминка окончена | Все лёгкие задачи | +200 |
| `HARD_FIRST` | Бесстрашный | Первая задача — сложная | +120 |
| `LESSON_CLEAR` | Зачистка подземелья | Все задачи урока | +100 |

### Как работает проверка достижений

```java
// GamificationService.checkAndGrantAchievements(user)

long solvedCount = submissionRepo.countDistinctTaskByUserIdAndStatus(
        user.getId(), SubmissionStatus.CORRECT);

// Проверяем каждое условие:
tryGrant(user, "FIRST_BLOOD", solvedCount >= 1, result);
tryGrant(user, "SOLVER_10",   solvedCount >= 10, result);
tryGrant(user, "SOLVER_50",   solvedCount >= 50, result);
```

`tryGrant()` — проверяет, не получено ли достижение ранее, и если нет — выдаёт:

```java
private void tryGrant(User user, String code, boolean condition, List<AchievementDto> result) {
    if (!condition) return;  // условие не выполнено — выходим

    achievementRepo.findByCode(code).ifPresent(achievement -> {
        // Проверяем, не получено ли уже
        if (!userAchievementRepo.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
            // Создаём запись
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepo.save(ua);

            // Начисляем бонусный XP
            addXp(user, achievement.getXpReward());

            // Добавляем в список "новых" для отображения тоста на фронтенде
            result.add(new AchievementDto(...));
        }
    });
}
```

---

## 11. База данных и миграции

### Flyway — контроль версий БД

Flyway отслеживает какие миграции уже выполнены через таблицу `flyway_schema_history`.

**Файлы миграций** лежат в `src/main/resources/db/migration/` и именуются:

```
V{номер}__{описание}.sql

Примеры:
  V1__init.sql              ← создание таблиц
  V2__add_streak.sql        ← добавление поля streak
  V3__create_challenges.sql ← новая таблица
```

**Правила:**
- **НЕЛЬЗЯ** изменять уже выполненную миграцию (контрольная сумма не совпадёт)
- Для изменений создавайте **новую** миграцию с увеличенным номером
- Два подчёркивания `__` после номера — обязательно

### Пример: добавить поле `streak` в таблицу `users`

Создайте файл `V2__add_streak.sql`:

```sql
ALTER TABLE users ADD COLUMN streak INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_solved_date DATE;
```

### H2 Console (для отладки)

При запуске доступна по адресу: http://localhost:8080/h2

```
JDBC URL:  jdbc:h2:file:./data/codequest
Username:  sa
Password:  (пустой)
```

Здесь можно выполнять SQL-запросы напрямую к базе.

### Сброс базы данных (при разработке)

Удалите папку `data/` и перезапустите приложение:

```bash
rm -rf data/
mvn spring-boot:run
```

Flyway заново выполнит миграции, `DataInitializer` создаст демо-данные.

---

## 12. Фронтенд

### Архитектура

Фронтенд — **не SPA-фреймворк**, а чистый HTML/CSS/JS, обслуживаемый Spring Boot как
статические файлы.

```
static/
├── index.html      ← Страница логина/регистрации (отдельная)
├── app.html        ← Основное приложение (псевдо-SPA)
├── css/style.css   ← Все стили
└── js/
    ├── api.js      ← Обёртка над Fetch API (общение с backend)
    └── app.js      ← Вся логика интерфейса
```

### Навигация (псевдо-SPA)

Вместо перезагрузки страниц используется **переключение блоков** (views):

```html
<!-- В app.html: -->
<div class="view active" id="view-quests">...</div>    <!-- Карта уроков -->
<div class="view" id="view-lesson">...</div>            <!-- Один урок -->
<div class="view" id="view-task">...</div>              <!-- Задача -->
<div class="view" id="view-achievements">...</div>      <!-- Достижения -->
<div class="view" id="view-admin">...</div>             <!-- Панель учителя -->
```

```css
/* style.css */
.view { display: none; }
.view.active { display: block; }
```

```javascript
// app.js — переключение:
function navigateTo(view) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById('view-' + view).classList.add('active');
    // ... обновление заголовка, загрузка данных
}
```

### api.js — обёртка для HTTP-запросов

```javascript
const API = {
    base: '/api',

    token() {
        return localStorage.getItem('cq_token');
    },

    headers() {
        const h = { 'Content-Type': 'application/json' };
        const t = this.token();
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    },

    async get(path) {
        const res = await fetch(this.base + path, { headers: this.headers() });
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';  // разлогин при истечении токена
            return;
        }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Ошибка');
        }
        return res.json();
    },

    async post(path, body) { /* аналогично, метод POST */ },
    async delete(path) { /* аналогично, метод DELETE */ }
};
```

**Использование в app.js:**

```javascript
// Загрузить уроки:
const lessons = await API.get('/lessons');

// Отправить решение:
const result = await API.post('/submissions', { taskId: 1, code: '...' });

// Создать урок (admin):
await API.post('/admin/lessons', { title: 'Новый урок', ... });
```

### Хранилище (localStorage)

| Ключ | Что хранит | Устанавливается |
|------|-----------|-----------------|
| `cq_token` | JWT-токен | При логине/регистрации |
| `cq_user` | Имя пользователя | При логине/регистрации |
| `cq_role` | Роль (STUDENT/TEACHER) | При логине/регистрации |

При нажатии «Выйти» — `localStorage.clear()` + redirect на `/index.html`.

### Тост-уведомления (при получении достижения)

```javascript
function showToast(icon, title, desc) {
    const t = document.createElement('div');
    t.className = 'toast';
    t.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <div class="toast-text">
            <h4>🎉 ${title}</h4>
            <p>${desc}</p>
        </div>`;
    document.getElementById('toasts').appendChild(t);
    setTimeout(() => t.remove(), 4000);  // исчезает через 4 сек
}
```

---

## 13. Рецепты расширения

### 13.1 Добавить новую сущность

**Задача:** добавить таблицу `challenges` (челленджи / соревнования).

**Шаг 1: Flyway-миграция** — `src/main/resources/db/migration/V2__create_challenges.sql`

```sql
CREATE TABLE challenges (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    task_id     BIGINT NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    bonus_xp    INT NOT NULL DEFAULT 50,
    created_by  BIGINT NOT NULL,
    FOREIGN KEY (task_id)    REFERENCES tasks(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

**Шаг 2: Entity** — `entity/Challenge.java`

```java
package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
public class Challenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "bonus_xp", nullable = false)
    private int bonusXp = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // геттеры и сеттеры...
}
```

**Шаг 3: Repository** — `repository/ChallengeRepository.java`

```java
package org.example.repository;

import org.example.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByEndTimeAfterOrderByStartTimeAsc(LocalDateTime now);
}
```

**Шаг 4: DTO** — `dto/ChallengeDto.java`

```java
package org.example.dto;

import java.time.LocalDateTime;

public record ChallengeDto(
    Long id, String title, String description,
    Long taskId, String taskTitle,
    LocalDateTime startTime, LocalDateTime endTime,
    int bonusXp
) {}
```

**Шаг 5: Service** — `service/ChallengeService.java`

```java
@Service
@Transactional(readOnly = true)
public class ChallengeService {
    private final ChallengeRepository challengeRepo;
    // конструктор, методы findActive(), create()...
}
```

**Шаг 6: Controller** — `controller/ChallengeController.java`

```java
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {
    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeDto>> getActive() { ... }
}
```

**Шаг 7: Разрешить доступ** в `SecurityConfig.java`:

```java
.requestMatchers(HttpMethod.GET, "/api/challenges/**").authenticated()
```

**Шаг 8: Фронтенд** — добавить view в `app.html`, логику в `app.js`.

---

### 13.2 Добавить новый API-эндпоинт

**Задача:** эндпоинт для истории отправок студента.

**Шаг 1: DTO** (если нужен новый формат ответа)

```java
public record SubmissionHistoryDto(
    Long id, String taskTitle, String status,
    String output, LocalDateTime submittedAt
) {}
```

**Шаг 2: Метод в сервисе**

```java
// В SubmissionService:
public List<SubmissionHistoryDto> getHistory(String username) {
    User user = userRepo.findByUsername(username).orElseThrow(...);
    return submissionRepo.findByUserIdOrderBySubmittedAtDesc(user.getId())
            .stream()
            .map(s -> new SubmissionHistoryDto(
                s.getId(), s.getTask().getTitle(),
                s.getStatus().name(), s.getOutput(), s.getSubmittedAt()
            ))
            .toList();
}
```

**Шаг 3: Эндпоинт в контроллере**

```java
// В SubmissionController:
@GetMapping("/history")
public ResponseEntity<List<SubmissionHistoryDto>> getHistory(Principal principal) {
    return ResponseEntity.ok(submissionService.getHistory(principal.getName()));
}
```

**Шаг 4: Вызов с фронтенда**

```javascript
const history = await API.get('/submissions/history');
```

---

### 13.3 Добавить новое достижение

**Шаг 1:** Добавьте запись в **новую** миграцию (`V2__new_achievements.sql`):

```sql
INSERT INTO achievements (code, name, description, icon, xp_reward, threshold) VALUES
    ('NIGHT_OWL', 'Полуночник', 'Решите задачу после полуночи', '🦉', 80, 1);
```

**Шаг 2:** Добавьте проверку в `GamificationService.checkAndGrantAchievements()`:

```java
// Проверка: решение отправлено после полуночи
boolean isNight = LocalDateTime.now().getHour() >= 0
               && LocalDateTime.now().getHour() < 5;
tryGrant(user, "NIGHT_OWL", isNight, newlyEarned);
```

Готово — фронтенд **автоматически** покажет тост с новым достижением.

---

### 13.4 Добавить новую страницу на фронтенде

**Задача:** страница «Лидерборд».

**Шаг 1:** Добавьте view-блок в `app.html`:

```html
<!-- после view-achievements -->
<div class="view" id="view-leaderboard">
    <div class="leaderboard-list" id="leaderboardList"></div>
</div>
```

**Шаг 2:** Добавьте кнопку навигации:

```html
<button class="nav-item" data-view="leaderboard">
    <span class="nav-icon">🏅</span> Лидерборд
</button>
```

**Шаг 3:** В `app.js` — обработка и загрузка данных:

```javascript
// В функции navigateTo() добавьте:
if (view === 'leaderboard') loadLeaderboard();

// Функция загрузки:
async function loadLeaderboard() {
    const list = document.getElementById('leaderboardList');
    try {
        const data = await API.get('/leaderboard');
        list.innerHTML = data.map((u, i) => `
            <div class="leaderboard-item">
                <span class="rank">#${i + 1}</span>
                <span class="name">${u.username}</span>
                <span class="xp">${u.xp} XP</span>
                <span class="level">LVL ${u.level}</span>
            </div>
        `).join('');
    } catch (e) {
        list.innerHTML = '<p>Ошибка загрузки</p>';
    }
}
```

**Шаг 4:** Стили в `css/style.css`:

```css
.leaderboard-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 20px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
    margin-bottom: 8px;
}
.leaderboard-item .rank {
    font-family: var(--pixel);
    font-size: 12px;
    color: var(--gold);
    width: 40px;
}
```

---

### 13.5 Добавить лидерборд

Полная реализация (backend + frontend):

**Backend:**

1. DTO:

```java
public record LeaderboardEntryDto(
    String username, int xp, int level, long solvedCount
) {}
```

2. В `ProfileController` (или новый `LeaderboardController`):

```java
@GetMapping("/api/leaderboard")
public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard() {
    List<User> top = userRepo.findTop20ByRoleOrderByXpDesc(Role.STUDENT);
    List<LeaderboardEntryDto> result = top.stream()
        .map(u -> new LeaderboardEntryDto(
            u.getUsername(), u.getXp(), u.getLevel(),
            submissionRepo.countDistinctTaskByUserIdAndStatus(u.getId(), SubmissionStatus.CORRECT)
        ))
        .toList();
    return ResponseEntity.ok(result);
}
```

3. Добавьте метод в `UserRepository`:

```java
List<User> findTop20ByRoleOrderByXpDesc(Role role);
```

4. Разрешите доступ в `SecurityConfig`:

```java
.requestMatchers(HttpMethod.GET, "/api/leaderboard").authenticated()
```

**Frontend:** — см. рецепт 13.4 выше.

---

### 13.6 Добавить streak (серию дней)

**Шаг 1: Миграция** — `V2__add_streak.sql`:

```sql
ALTER TABLE users ADD COLUMN streak INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_solved_date DATE;
```

**Шаг 2: Обновить Entity** — `User.java`:

```java
@Column(nullable = false)
private int streak = 0;

@Column(name = "last_solved_date")
private LocalDate lastSolvedDate;

// геттеры и сеттеры
```

**Шаг 3: Логика в GamificationService:**

```java
@Transactional
public void updateStreak(User user) {
    LocalDate today = LocalDate.now();
    LocalDate lastDate = user.getLastSolvedDate();

    if (lastDate == null || lastDate.isBefore(today.minusDays(1))) {
        user.setStreak(1);                    // сброс серии
    } else if (lastDate.equals(today.minusDays(1))) {
        user.setStreak(user.getStreak() + 1); // +1 к серии
    }
    // если lastDate == today → серия не меняется (уже решал сегодня)

    user.setLastSolvedDate(today);
    userRepo.save(user);
}
```

**Шаг 4:** Вызовите `updateStreak(user)` в `SubmissionService.submit()` при правильном ответе.

**Шаг 5:** Добавьте `streak` в `ProfileDto` и обновите фронтенд.

---

### 13.7 Добавить челленджи

Полная реализация описана в рецепте 13.1. Ключевые моменты:

1. Таблица `challenges` (id, title, task_id, start_time, end_time, bonus_xp)
2. Преподаватель создаёт челлендж через `/api/admin/challenges`
3. Студент видит активные челленджи (где `end_time > now()`)
4. При решении задачи, если она привязана к активному челленджу — бонусные XP
5. Добавить проверку в `SubmissionService`:

```java
// После успешного решения:
List<Challenge> activeChallenges = challengeRepo
    .findByTaskIdAndEndTimeAfter(task.getId(), LocalDateTime.now());
for (Challenge ch : activeChallenges) {
    gamificationService.addXp(user, ch.getBonusXp());
}
```

---

## 14. Переключение на PostgreSQL

### Шаг 1: Установите PostgreSQL (или используйте Docker)

```bash
# Только БД через Docker:
docker run -d --name codequest-db \
    -e POSTGRES_DB=codequest \
    -e POSTGRES_USER=codequest \
    -e POSTGRES_PASSWORD=codequest \
    -p 5432:5432 \
    postgres:16-alpine
```

### Шаг 2: Запустите с профилем `postgres`

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Шаг 3: Или через Docker Compose (app + db)

```bash
docker-compose up --build
```

### Различия синтаксиса H2 vs PostgreSQL

При написании миграций учитывайте:

| Функция | H2 | PostgreSQL |
|---------|----|----|
| Автоинкремент | `BIGINT AUTO_INCREMENT` | `BIGSERIAL` или `BIGINT GENERATED ALWAYS AS IDENTITY` |
| Длинный текст | `CLOB` | `TEXT` |
| Текущее время | `CURRENT_TIMESTAMP` | `CURRENT_TIMESTAMP` (совпадает) |

Для совместимости лучше использовать отдельные миграции:
- `db/migration/V1__init.sql` — для H2 (по умолчанию)
- Для PostgreSQL — либо адаптируйте синтаксис, либо используйте
  Flyway-свойство `spring.flyway.locations` для разных профилей.

---

## 15. Типичные ошибки и отладка

### «401 Unauthorized» на всех запросах

- Проверьте, что отправляете заголовок `Authorization: Bearer <token>`
- Проверьте, не истёк ли токен (по умолчанию — 24 часа)
- Откройте DevTools → Network → посмотрите заголовки запроса

### «Flyway migration checksum mismatch»

Вы изменили уже выполненную миграцию. Решение:
1. Удалите папку `data/` (сброс БД)
2. Или запустите Flyway repair: `mvn flyway:repair`

### Приложение не запускается: «Port 8080 already in use»

```bash
# Найти процесс:
lsof -i :8080

# Убить его:
kill -9 <PID>
```

### Сущность не маппится на таблицу

Убедитесь, что:
1. Имя таблицы в `@Table(name = "...")` совпадает с SQL в миграции
2. Имена столбцов в `@Column(name = "...")` совпадают
3. Тип данных совместим (String → VARCHAR, int → INT, LocalDateTime → TIMESTAMP)

### Как включить логирование SQL-запросов

В `application.yml`:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Как тестировать API через curl

```bash
# Логин:
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"student"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Профиль:
curl -s http://localhost:8080/api/profile \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Отправить решение:
curl -s http://localhost:8080/api/submissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"taskId":1,"code":"#include <stdio.h>\nint main(){printf(\"Hello, World!\");return 0;}"}' \
  | python3 -m json.tool
```

---

## Чек-лист: «Хочу добавить фичу X»

```
□ 1. Нужна новая таблица?  → Создать Flyway-миграцию (V{N}__описание.sql)
□ 2. Нужна новая сущность? → Создать Entity-класс в entity/
□ 3. Нужен доступ к данным? → Создать Repository-интерфейс в repository/
□ 4. Нужна бизнес-логика?  → Создать/дополнить Service в service/
□ 5. Нужен API-эндпоинт?   → Создать/дополнить Controller в controller/
□ 6. Нужен новый формат данных? → Создать DTO (record) в dto/
□ 7. Нужно ограничить доступ? → Настроить в SecurityConfig.java
□ 8. Нужен UI?              → Добавить view в app.html + логику в app.js
□ 9. Удалить data/ и перезапустить для тестирования
```

---

*Документация актуальна для версии 1.0 проекта CodeQuest.*

# CodeQuest — Платформа для обучения программированию с геймификацией

Веб-приложение для обучения программированию на языке C с элементами геймификации.
Студенты проходят теорию, решают задачи и получают опыт/достижения.
Преподаватели управляют контентом и контролируют прогресс.

## Технологический стек

| Компонент             | Технология                         |
|-----------------------|------------------------------------|
| Язык                  | Java 17                            |
| Фреймворк             | Spring Boot 3 (Web, Security, JPA) |
| Аутентификация        | Spring Security + JWT              |
| ORM                   | Spring Data JPA / Hibernate        |
| База данных (dev)     | H2 (file-based)                    |
| База данных (prod)    | PostgreSQL 16                      |
| Миграции              | Flyway                             |
| Фронтенд             | HTML + CSS + JavaScript (Fetch API)|
| Контейнеризация       | Docker, Docker Compose             |

## Структура проекта

```
src/main/java/org/example/
├── CodeQuestApplication.java     — точка входа
├── config/
│   ├── SecurityConfig.java       — конфигурация Spring Security
│   ├── JwtAuthFilter.java        — JWT-фильтр
│   └── DataInitializer.java      — заполнение БД демо-данными
├── entity/                       — JPA-сущности
├── repository/                   — Spring Data репозитории
├── service/                      — бизнес-логика
│   ├── AuthService.java          — регистрация / вход
│   ├── LessonService.java        — управление уроками
│   ├── TaskService.java          — управление задачами
│   ├── SubmissionService.java    — проверка решений
│   ├── GamificationService.java  — XP, уровни, достижения
│   └── JwtService.java           — генерация / валидация JWT
├── controller/                   — REST API контроллеры
├── dto/                          — Data Transfer Objects (records)
└── exception/                    — глобальная обработка ошибок

src/main/resources/
├── application.yml               — конфигурация приложения
├── db/migration/V1__init.sql     — Flyway-миграция
└── static/                       — фронтенд (HTML/CSS/JS)
```

## Быстрый старт

### Вариант 1: Локальный запуск (H2)

```bash
./mvnw spring-boot:run
```

Приложение будет доступно по адресу: http://localhost:8080

### Вариант 2: Docker Compose (PostgreSQL)

```bash
docker-compose up --build
```

### Демо-аккаунты

| Роль          | Логин     | Пароль    |
|---------------|-----------|-----------|
| Преподаватель | teacher   | teacher   |
| Студент       | student   | student   |

## API Endpoints

| Метод  | URL                          | Описание                    | Доступ     |
|--------|------------------------------|-----------------------------|------------|
| POST   | `/api/auth/register`         | Регистрация                 | Все        |
| POST   | `/api/auth/login`            | Вход                        | Все        |
| GET    | `/api/profile`               | Профиль текущего пользователя | Auth     |
| GET    | `/api/lessons`               | Список уроков               | Auth       |
| GET    | `/api/lessons/{id}`          | Урок по ID                  | Auth       |
| GET    | `/api/tasks/lesson/{id}`     | Задачи урока                | Auth       |
| GET    | `/api/tasks/{id}`            | Задача по ID                | Auth       |
| POST   | `/api/submissions`           | Отправить решение           | Auth       |
| POST   | `/api/admin/lessons`         | Создать урок                | TEACHER    |
| DELETE | `/api/admin/lessons/{id}`    | Удалить урок                | TEACHER    |
| POST   | `/api/admin/tasks`           | Создать задачу              | TEACHER    |
| DELETE | `/api/admin/tasks/{id}`      | Удалить задачу              | TEACHER    |

## Геймификация

- **XP (опыт)** — начисляется за правильные решения задач
- **Уровни** — рассчитываются по формуле `level = floor(sqrt(xp / 100)) + 1`
- **Достижения** — автоматически выдаются при выполнении условий:
  - ⚔️ Первая кровь — решите первую задачу
  - 🔧 Подмастерье — решите 10 задач
  - 👨‍💻 Мастер кода — решите 50 задач
  - и другие

## Архитектура

Приложение построено по классической трёхслойной архитектуре:

```
[Frontend: HTML/CSS/JS] → [REST API (Controllers)] → [Service Layer] → [Repository/JPA] → [Database]
                                                           ↓
                                                   [GamificationService]
```

Аутентификация реализована через stateless JWT-токены.

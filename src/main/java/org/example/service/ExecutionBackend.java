package org.example.service;

/**
 * Где фактически выполнялись компиляция и запуск студенческого кода.
 */
public enum ExecutionBackend {
    /** Контейнер с образом из app.code-execution.docker-image */
    DOCKER,
    /** Локальный gcc на машине с JVM (fallback) */
    LOCAL_GCC,
    /** Проверка отключена в конфигурации */
    DISABLED,
    /** Не удалось выполнить docker run (демон, образ, ошибка запуска контейнера) */
    DOCKER_RUN_FAILED
}

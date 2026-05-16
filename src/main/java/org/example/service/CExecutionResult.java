package org.example.service;

public record CExecutionResult(
        Kind kind,
        String stdout,
        String stderrOrCompileLog,
        ExecutionBackend sandboxBackend
) {
    public enum Kind {
        OK,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        TIMEOUT,
        DOCKER_ERROR,
        DISABLED
    }

    public static CExecutionResult disabled() {
        return new CExecutionResult(Kind.DISABLED, "", "Проверка кода отключена (app.code-execution.enabled=false).",
                ExecutionBackend.DISABLED);
    }

    public static CExecutionResult dockerError(String msg) {
        return new CExecutionResult(Kind.DOCKER_ERROR, "", msg != null ? msg : "Ошибка Docker",
                ExecutionBackend.DOCKER_RUN_FAILED);
    }
}

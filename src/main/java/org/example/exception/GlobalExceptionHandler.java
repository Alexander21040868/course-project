package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Не найдено";
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", msg));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenOperationException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Действие запрещено";
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Неверный запрос";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", msg));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException e) {
        String path = e.getResourcePath();
        String hint = path.startsWith("/api/")
                ? "Нет обработчика для этого API. Остановите старый процесс Java и запустите приложение заново после mvn package (или Reload в IDE)."
                : "Ресурс не найден.";
        log.warn("404 {} — {}", path, hint);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", hint));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        String detail = e.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = e.getClass().getSimpleName();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + detail));
    }
}

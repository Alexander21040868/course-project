package org.example.service;

import org.example.dto.HintRequest;
import org.example.entity.Task;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeHintServiceTest {

    @Test
    void suggestsIncludeWhenMissing() {
        Task t = new Task();
        t.setDescription("Выведите число");
        t.setHints(null);
        var svc = new CodeHintService(id -> id == 1L ? Optional.of(t) : Optional.empty());

        var r = svc.hint(new HintRequest(1L, "int main() { return 0; }", null));

        assertTrue(r.hint().contains("#include") || r.hint().contains("printf"));
    }

    @Test
    void usesTaskHintWhenNoRulesMatch() {
        Task t = new Task();
        t.setDescription("x");
        t.setHints("Подсказка из БД");
        var map = Map.of(2L, t);
        var svc = new CodeHintService(id -> Optional.ofNullable(map.get(id)));

        var r = svc.hint(new HintRequest(2L, """
                #include <stdio.h>
                int main() { return 0; }
                """, null));

        assertEquals("Подсказка из БД", r.hint());
    }
}

package org.example.service;

import org.example.dto.HintRequest;
import org.example.entity.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeHintServiceTest {

    @Test
    void suggestsIncludeWhenMissing() {
        Task t = new Task();
        t.setDescription("Выведите число");
        t.setHints(null);
        var svc = new CodeHintService((id, u) -> id == 1L ? t : null);

        var r = svc.hint(new HintRequest(1L, "int main() { return 0; }", null), "any");

        assertTrue(r.hint().contains("#include") || r.hint().contains("printf"));
    }

    @Test
    void usesTaskHintWhenNoRulesMatch() {
        Task t = new Task();
        t.setDescription("x");
        t.setHints("Подсказка из БД");
        var svc = new CodeHintService((id, u) -> id == 2L ? t : null);

        var r = svc.hint(new HintRequest(2L, """
                #include <stdio.h>
                int main() { return 0; }
                """, null), "u");

        assertEquals("Подсказка из БД", r.hint());
    }
}

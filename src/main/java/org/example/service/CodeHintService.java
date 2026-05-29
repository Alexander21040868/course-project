package org.example.service;

import org.example.dto.HintRequest;
import org.example.dto.HintResponse;
import org.example.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Service
@Transactional(readOnly = true)
public class CodeHintService {

    private final BiFunction<Long, String, Task> resolveTask;

    @Autowired
    public CodeHintService(TaskService taskService) {
        this.resolveTask = taskService::requireTaskForLearner;
    }

    CodeHintService(BiFunction<Long, String, Task> resolveTask) {
        this.resolveTask = resolveTask;
    }

    public HintResponse hint(HintRequest req, String username) {
        Task task = resolveTask.apply(req.taskId(), username);
        String code = req.code() == null ? "" : req.code();
        String out = req.output() == null ? "" : req.output().toLowerCase();

        List<String> tips = new ArrayList<>();

        if (!code.contains("#include")) tips.add("Подключите заголовок, например `#include <stdio.h>`.");
        if (code.contains("int main") && !code.contains("return")) tips.add("В конце `main` обычно пишут `return 0;`.");
        if (code.contains("printf") && !code.contains("#include <stdio.h>"))
            tips.add("Для `printf` нужен `#include <stdio.h>`.");
        if (!code.contains(";") && code.lines().anyMatch(l -> l.matches(".*(printf|return|int main).*")))
            tips.add("Проверьте точки с запятой после операторов.");

        String desc = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
        if (desc.contains("scanf") && !code.contains("scanf")) tips.add("По условию может понадобиться `scanf` для ввода.");
        if ((desc.contains("цикл") || desc.contains("for") || desc.contains("while"))
                && !code.contains("for") && !code.contains("while"))
            tips.add("Задача про цикл — подумайте о `for` или `while`.");

        if (out.contains("тест") || out.contains("неверно") || out.contains("fail")) {
            tips.add("Сравните вывод с примерами тестов: пробелы и переносы строк должны совпадать.");
        }

        if (tips.isEmpty()) {
            String tail = task.getHints() != null && !task.getHints().isBlank()
                    ? task.getHints()
                    : "Перечитайте условие и проверьте формат вывода (printf, \\n).";
            return new HintResponse(tail);
        }
        return new HintResponse(String.join(" ", tips));
    }
}

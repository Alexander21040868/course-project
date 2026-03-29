package org.example.config;

import org.example.entity.*;
import org.example.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Заполняет БД демо-данными при первом запуске.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo, LessonRepository lessonRepo,
                           TaskRepository taskRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // --- Users ---
        User teacher = new User("teacher", "teacher@codequest.dev", encoder.encode("teacher"), Role.TEACHER);
        User student = new User("student", "student@codequest.dev", encoder.encode("student"), Role.STUDENT);
        userRepo.save(teacher);
        userRepo.save(student);

        // --- Lesson 1 ---
        Lesson l1 = new Lesson();
        l1.setTitle("Введение в C");
        l1.setDescription("Основы языка C: первая программа, компиляция и запуск.");
        l1.setContent(
            "<h3>Что такое C?</h3>" +
            "<p>C — один из старейших и самых влиятельных языков программирования. " +
            "Он был создан Деннисом Ритчи в 1972 году в Bell Labs.</p>" +
            "<h3>Первая программа</h3>" +
            "<pre><code>#include &lt;stdio.h&gt;\n\nint main() {\n    printf(\"Hello, World!\\n\");\n    return 0;\n}</code></pre>" +
            "<p><code>#include &lt;stdio.h&gt;</code> подключает стандартную библиотеку ввода-вывода. " +
            "Функция <code>main()</code> — точка входа в программу.</p>" +
            "<h3>Компиляция</h3>" +
            "<pre><code>gcc hello.c -o hello\n./hello</code></pre>"
        );
        l1.setOrderIndex(0);
        l1.setAuthor(teacher);
        lessonRepo.save(l1);

        // --- Tasks for Lesson 1 ---
        createTask(l1, "Hello, World!", 
            "Напишите программу на C, которая выводит <code>Hello, World!</code> на экран.",
            Difficulty.EASY, 15,
            "#include <stdio.h>\n\nint main() {\n    // Ваш код здесь\n    return 0;\n}",
            "Hello, World!", "Используйте функцию printf()", 0);

        createTask(l1, "Сумма двух чисел",
            "Напишите программу, которая объявляет две переменные <code>a = 5</code> и <code>b = 3</code>, " +
            "вычисляет их сумму и выводит результат: <code>8</code>",
            Difficulty.EASY, 20,
            "#include <stdio.h>\n\nint main() {\n    int a = 5;\n    int b = 3;\n    // Вычислите и выведите сумму\n    return 0;\n}",
            "8", "printf(\"%d\", a + b)", 1);

        createTask(l1, "Таблица умножения",
            "Напишите программу, которая выводит таблицу умножения на 7 (от 7*1 до 7*10). " +
            "Формат вывода: <code>7 x 1 = 7</code>",
            Difficulty.MEDIUM, 35,
            "#include <stdio.h>\n\nint main() {\n    // Используйте цикл for\n    return 0;\n}",
            "7 x 1 = 7", "Используйте цикл for от 1 до 10", 2);

        // --- Lesson 2 ---
        Lesson l2 = new Lesson();
        l2.setTitle("Условия и циклы");
        l2.setDescription("Управляющие конструкции: if/else, switch, while, for.");
        l2.setContent(
            "<h3>Условный оператор if/else</h3>" +
            "<pre><code>if (x > 0) {\n    printf(\"Положительное\\n\");\n} else {\n    printf(\"Неположительное\\n\");\n}</code></pre>" +
            "<h3>Цикл for</h3>" +
            "<pre><code>for (int i = 0; i < 10; i++) {\n    printf(\"%d \", i);\n}</code></pre>" +
            "<h3>Цикл while</h3>" +
            "<pre><code>int n = 10;\nwhile (n > 0) {\n    printf(\"%d \", n);\n    n--;\n}</code></pre>"
        );
        l2.setOrderIndex(1);
        l2.setAuthor(teacher);
        lessonRepo.save(l2);

        createTask(l2, "Чётное или нечётное?",
            "Напишите программу, которая проверяет число <code>n = 42</code> и выводит " +
            "<code>even</code> если оно чётное или <code>odd</code> если нечётное.",
            Difficulty.EASY, 15,
            "#include <stdio.h>\n\nint main() {\n    int n = 42;\n    // Проверьте чётность\n    return 0;\n}",
            "even", "Используйте оператор % (остаток от деления)", 0);

        createTask(l2, "Числа Фибоначчи",
            "Выведите первые 10 чисел Фибоначчи через пробел: <code>0 1 1 2 3 5 8 13 21 34</code>",
            Difficulty.HARD, 50,
            "#include <stdio.h>\n\nint main() {\n    // Вычислите и выведите числа Фибоначчи\n    return 0;\n}",
            "0 1 1 2 3 5 8 13 21 34", "Храните два предыдущих числа в переменных", 1);

        // --- Lesson 3 ---
        Lesson l3 = new Lesson();
        l3.setTitle("Массивы и строки");
        l3.setDescription("Работа с массивами, строками и указателями.");
        l3.setContent(
            "<h3>Массивы</h3>" +
            "<pre><code>int arr[5] = {1, 2, 3, 4, 5};\nfor (int i = 0; i < 5; i++) {\n    printf(\"%d \", arr[i]);\n}</code></pre>" +
            "<h3>Строки</h3>" +
            "<p>В C строки — это массивы символов, оканчивающиеся нулевым символом <code>'\\0'</code>.</p>" +
            "<pre><code>char name[] = \"CodeQuest\";\nprintf(\"%s\\n\", name);</code></pre>" +
            "<h3>Указатели</h3>" +
            "<pre><code>int x = 10;\nint *p = &x;\nprintf(\"Адрес: %p, Значение: %d\\n\", p, *p);</code></pre>"
        );
        l3.setOrderIndex(2);
        l3.setAuthor(teacher);
        lessonRepo.save(l3);

        createTask(l3, "Максимум в массиве",
            "Напишите программу, которая находит максимальный элемент в массиве " +
            "<code>{3, 7, 2, 9, 5}</code> и выводит его.",
            Difficulty.MEDIUM, 30,
            "#include <stdio.h>\n\nint main() {\n    int arr[] = {3, 7, 2, 9, 5};\n    int n = 5;\n    // Найдите максимум\n    return 0;\n}",
            "9", "Пройдите по массиву, сравнивая каждый элемент с текущим максимумом", 0);

        createTask(l3, "Реверс строки",
            "Напишите программу, которая переворачивает строку <code>\"hello\"</code> и выводит <code>\"olleh\"</code>.",
            Difficulty.HARD, 50,
            "#include <stdio.h>\n#include <string.h>\n\nint main() {\n    char str[] = \"hello\";\n    // Переверните строку\n    return 0;\n}",
            "olleh", "Используйте два индекса: начало и конец строки", 1);
    }

    private void createTask(Lesson lesson, String title, String desc,
                             Difficulty diff, int xp, String template,
                             String expected, String hints, int order) {
        Task t = new Task();
        t.setLesson(lesson);
        t.setTitle(title);
        t.setDescription(desc);
        t.setDifficulty(diff);
        t.setXpReward(xp);
        t.setTemplateCode(template);
        t.setExpectedOutput(expected);
        t.setHints(hints);
        t.setOrderIndex(order);
        taskRepo.save(t);
    }
}

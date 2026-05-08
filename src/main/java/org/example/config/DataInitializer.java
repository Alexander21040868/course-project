package org.example.config;

import org.example.entity.*;
import org.example.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final TestCaseRepository testCaseRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo, LessonRepository lessonRepo,
                           TaskRepository taskRepo, TestCaseRepository testCaseRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.testCaseRepo = testCaseRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        User teacher = new User("teacher", "teacher@codequest.dev", encoder.encode("teacher"), Role.TEACHER);
        User student = new User("student", "student@codequest.dev", encoder.encode("student"), Role.STUDENT);
        userRepo.save(teacher);
        userRepo.save(student);

//        Lesson l1 = lesson(teacher, 0, "Введение в C",
//            "Первая программа, printf, компиляция и запуск.",
//            "<h3>Что такое C?</h3>" +
//            "<p>C — один из старейших и самых влиятельных языков. Создан Деннисом Ритчи в 1972 году в Bell Labs. " +
//            "На нём написаны ядро Linux, базы данных и встраиваемые системы.</p>" +
//            "<h3>Первая программа</h3>" +
//            "<pre><code>#include &lt;stdio.h&gt;\n\nint main() {\n    printf(\"Hello, World!\\n\");\n    return 0;\n}</code></pre>" +
//            "<p><code>#include &lt;stdio.h&gt;</code> — подключает библиотеку ввода/вывода. " +
//            "<code>main()</code> — точка входа. <code>printf()</code> — вывод текста.</p>" +
//            "<h3>Компиляция</h3><pre><code>gcc hello.c -o hello\n./hello</code></pre>");
//
//        task(l1, 0, "Hello, World!", "Выведите <code>Hello, World!</code> на экран.",
//            Difficulty.EASY, 15, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "Hello, World!", "Используйте printf()",
//            new String[]{null, "Hello, World!", "true"},
//            new String[]{null, "Hello, World!", "false"});
//
//        task(l1, 1, "Вывод нескольких строк", "Выведите две строки:\n<code>Hello</code>\n<code>World</code>",
//            Difficulty.EASY, 15, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "Hello\nWorld", "Два вызова printf() с \\n",
//            new String[]{null, "Hello\nWorld", "true"});
//
//        task(l1, 2, "Сумма двух чисел", "Объявите <code>a = 5</code> и <code>b = 3</code>, выведите их сумму.",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\nint main() {\n    int a = 5;\n    int b = 3;\n    \n    return 0;\n}",
//            "8", "printf(\"%d\", a + b)",
//            new String[]{null, "8", "true"});
//
//        task(l1, 3, "Площадь прямоугольника", "Ширина=7, высота=4. Выведите площадь.",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\nint main() {\n    int w = 7, h = 4;\n    \n    return 0;\n}",
//            "28", "printf(\"%d\", w * h)",
//            new String[]{null, "28", "true"});
//
//        Lesson l2 = lesson(teacher, 1, "Переменные и типы данных",
//            "int, float, double, char и форматированный вывод.",
//            "<h3>Основные типы</h3>" +
//            "<table><tr><th>Тип</th><th>Размер</th><th>Пример</th></tr>" +
//            "<tr><td>int</td><td>4 байта</td><td>42</td></tr>" +
//            "<tr><td>float</td><td>4 байта</td><td>3.14f</td></tr>" +
//            "<tr><td>double</td><td>8 байт</td><td>3.14159</td></tr>" +
//            "<tr><td>char</td><td>1 байт</td><td>'A'</td></tr></table>" +
//            "<h3>Форматированный вывод</h3>" +
//            "<pre><code>int x = 10;\nfloat pi = 3.14f;\nchar ch = 'A';\nprintf(\"x=%d, pi=%.2f, ch=%c\\n\", x, pi, ch);</code></pre>");
//
//        task(l2, 0, "Обмен переменных", "Даны <code>a=10, b=20</code>. Поменяйте их местами и выведите <code>a</code> затем <code>b</code>.",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\nint main() {\n    int a = 10, b = 20;\n    \n    printf(\"%d %d\", a, b);\n    return 0;\n}",
//            "20 10", "Используйте третью переменную temp",
//            new String[]{null, "20 10", "true"});
//
//        task(l2, 1, "Преобразование температуры", "Переведите 100°F в Цельсии. Формула: C = (F-32)*5/9. Выведите целую часть.",
//            Difficulty.MEDIUM, 25, "#include <stdio.h>\n\nint main() {\n    int f = 100;\n    \n    return 0;\n}",
//            "37", "Используйте целочисленное деление или приведение типа",
//            new String[]{null, "37", "true"});
//
//        task(l2, 2, "ASCII код символа", "Выведите ASCII-код символа <code>'Z'</code>.",
//            Difficulty.EASY, 15, "#include <stdio.h>\n\nint main() {\n    char ch = 'Z';\n    \n    return 0;\n}",
//            "90", "printf(\"%d\", ch) — char автоматически приводится к int",
//            new String[]{null, "90", "true"});
//
//        Lesson l3 = lesson(teacher, 2, "Условия и ветвления",
//            "Операторы if/else, switch, тернарный оператор.",
//            "<h3>if / else</h3>" +
//            "<pre><code>if (x > 0) {\n    printf(\"Positive\");\n} else if (x == 0) {\n    printf(\"Zero\");\n} else {\n    printf(\"Negative\");\n}</code></pre>" +
//            "<h3>Тернарный оператор</h3>" +
//            "<pre><code>int max = (a > b) ? a : b;</code></pre>" +
//            "<h3>switch</h3>" +
//            "<pre><code>switch (day) {\n    case 1: printf(\"Mon\"); break;\n    case 2: printf(\"Tue\"); break;\n    default: printf(\"Other\");\n}</code></pre>");
//
//        task(l3, 0, "Чётное или нечётное?", "Проверьте <code>n = 42</code>. Выведите <code>even</code> или <code>odd</code>.",
//            Difficulty.EASY, 15, "#include <stdio.h>\n\nint main() {\n    int n = 42;\n    \n    return 0;\n}",
//            "even", "n % 2 == 0 → even",
//            new String[]{null, "even", "true"});
//
//        task(l3, 1, "Максимум из трёх", "Даны <code>a=7, b=15, c=9</code>. Выведите максимальное.",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\nint main() {\n    int a=7, b=15, c=9;\n    \n    return 0;\n}",
//            "15", "Вложенные if или тернарный оператор",
//            new String[]{null, "15", "true"});
//
//        task(l3, 2, "Оценка по баллам", "Балл=85. Выведите: >=90 → A, >=80 → B, >=70 → C, иначе → F.",
//            Difficulty.MEDIUM, 25, "#include <stdio.h>\n\nint main() {\n    int score = 85;\n    \n    return 0;\n}",
//            "B", "Используйте цепочку if/else if/else",
//            new String[]{null, "B", "true"});
//
//        task(l3, 3, "Високосный год", "Год=2024. Выведите <code>leap</code> если високосный, иначе <code>common</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint main() {\n    int year = 2024;\n    \n    return 0;\n}",
//            "leap", "Кратен 4 и не 100, или кратен 400",
//            new String[]{null, "leap", "true"});
//
//        Lesson l4 = lesson(teacher, 3, "Циклы",
//            "for, while, do-while, break и continue.",
//            "<h3>Цикл for</h3>" +
//            "<pre><code>for (int i = 0; i &lt; 5; i++) {\n    printf(\"%d \", i);\n}</code></pre>" +
//            "<h3>Цикл while</h3>" +
//            "<pre><code>int n = 5;\nwhile (n > 0) {\n    printf(\"%d \", n);\n    n--;\n}</code></pre>" +
//            "<h3>break и continue</h3>" +
//            "<pre><code>for (int i = 0; i &lt; 10; i++) {\n    if (i == 5) break;\n    if (i % 2 == 0) continue;\n    printf(\"%d \", i);\n}</code></pre>");
//
//        task(l4, 0, "Сумма от 1 до N", "Вычислите сумму от 1 до 100. Выведите результат.",
//            Difficulty.EASY, 15, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "5050", "Цикл for от 1 до 100, суммируйте в переменную",
//            new String[]{null, "5050", "true"});
//
//        task(l4, 1, "Таблица умножения", "Выведите таблицу умножения на 7 (от 1 до 10). Формат: <code>7 x 1 = 7</code> (каждая с новой строки).",
//            Difficulty.MEDIUM, 35, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "7 x 1 = 7", "Цикл for от 1 до 10",
//            new String[]{null, "7 x 1 = 7", "true"});
//
//        task(l4, 2, "Факториал", "Вычислите 10! (факториал 10) и выведите результат.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "3628800", "Цикл от 1 до 10, умножайте в переменную-аккумулятор",
//            new String[]{null, "3628800", "true"});
//
//        task(l4, 3, "Числа Фибоначчи", "Выведите первые 10 чисел Фибоначчи через пробел.",
//            Difficulty.HARD, 50, "#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}",
//            "0 1 1 2 3 5 8 13 21 34", "Храните два предыдущих числа",
//            new String[]{null, "0 1 1 2 3 5 8 13 21 34", "true"});
//
//        Lesson l5 = lesson(teacher, 4, "Функции",
//            "Объявление, параметры, возвращаемое значение, рекурсия.",
//            "<h3>Объявление функции</h3>" +
//            "<pre><code>int add(int a, int b) {\n    return a + b;\n}</code></pre>" +
//            "<h3>Прототипы</h3>" +
//            "<p>Объявление до main(), определение после.</p>" +
//            "<h3>Рекурсия</h3>" +
//            "<pre><code>int factorial(int n) {\n    if (n &lt;= 1) return 1;\n    return n * factorial(n - 1);\n}</code></pre>");
//
//        task(l5, 0, "Функция max", "Напишите функцию <code>int max(int a, int b)</code>. В main вызовите <code>max(10, 25)</code> и выведите результат.",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\n\nint main() {\n    printf(\"%d\", max(10, 25));\n    return 0;\n}",
//            "25", "return (a > b) ? a : b;",
//            new String[]{null, "25", "true"});
//
//        task(l5, 1, "Функция степени", "Напишите функцию <code>int power(int base, int exp)</code>. Выведите <code>power(2, 10)</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\n\nint main() {\n    printf(\"%d\", power(2, 10));\n    return 0;\n}",
//            "1024", "Цикл, умножающий result на base exp раз",
//            new String[]{null, "1024", "true"});
//
//        task(l5, 2, "Рекурсивный факториал", "Напишите рекурсивную функцию <code>factorial(n)</code>. Выведите <code>factorial(7)</code>.",
//            Difficulty.HARD, 45, "#include <stdio.h>\n\n\nint main() {\n    printf(\"%d\", factorial(7));\n    return 0;\n}",
//            "5040", "Базовый случай: n <= 1 → return 1",
//            new String[]{null, "5040", "true"});
//
//        Lesson l6 = lesson(teacher, 5, "Массивы",
//            "Одномерные массивы, обход, поиск, сортировка.",
//            "<h3>Объявление</h3>" +
//            "<pre><code>int arr[5] = {3, 1, 4, 1, 5};</code></pre>" +
//            "<h3>Обход</h3>" +
//            "<pre><code>for (int i = 0; i &lt; 5; i++) {\n    printf(\"%d \", arr[i]);\n}</code></pre>");
//
//        task(l6, 0, "Максимум в массиве", "Найдите максимум в <code>{3, 7, 2, 9, 5}</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint main() {\n    int arr[] = {3, 7, 2, 9, 5};\n    int n = 5;\n    \n    return 0;\n}",
//            "9", "Пройдите по массиву, сравнивая с текущим max",
//            new String[]{null, "9", "true"});
//
//        task(l6, 1, "Среднее арифметическое", "Массив <code>{10, 20, 30, 40, 50}</code>. Выведите среднее (целое).",
//            Difficulty.EASY, 20, "#include <stdio.h>\n\nint main() {\n    int arr[] = {10, 20, 30, 40, 50};\n    int n = 5;\n    \n    return 0;\n}",
//            "30", "Суммируйте и разделите на n",
//            new String[]{null, "30", "true"});
//
//        task(l6, 2, "Реверс массива", "Переверните <code>{1,2,3,4,5}</code> и выведите через пробел.",
//            Difficulty.MEDIUM, 35, "#include <stdio.h>\n\nint main() {\n    int arr[] = {1, 2, 3, 4, 5};\n    int n = 5;\n    \n    return 0;\n}",
//            "5 4 3 2 1", "Два указателя: начало и конец",
//            new String[]{null, "5 4 3 2 1", "true"});
//
//        task(l6, 3, "Сортировка пузырьком", "Отсортируйте <code>{5,3,8,1,2}</code> и выведите.",
//            Difficulty.HARD, 50, "#include <stdio.h>\n\nint main() {\n    int arr[] = {5, 3, 8, 1, 2};\n    int n = 5;\n    \n    return 0;\n}",
//            "1 2 3 5 8", "Вложенные циклы, обмен соседних элементов",
//            new String[]{null, "1 2 3 5 8", "true"});
//
//        Lesson l7 = lesson(teacher, 6, "Строки",
//            "Символьные массивы, strlen, strcmp, strcpy.",
//            "<h3>Строки в C</h3>" +
//            "<p>Строка — массив <code>char</code>, оканчивающийся <code>'\\0'</code>.</p>");
//
//        task(l7, 0, "Длина строки", "Выведите длину строки <code>\"CodeQuest\"</code> (без string.h, вручную).",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint main() {\n    char s[] = \"CodeQuest\";\n    \n    return 0;\n}",
//            "9", "Цикл while (s[i] != '\\0') i++;",
//            new String[]{null, "9", "true"});
//
//        task(l7, 1, "Реверс строки", "Переверните <code>\"hello\"</code> и выведите.",
//            Difficulty.HARD, 50, "#include <stdio.h>\n#include <string.h>\n\nint main() {\n    char str[] = \"hello\";\n    \n    return 0;\n}",
//            "olleh", "Два индекса: начало и конец",
//            new String[]{null, "olleh", "true"});
//
//        task(l7, 2, "Подсчёт гласных", "Посчитайте гласные (aeiou) в <code>\"programming\"</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint main() {\n    char s[] = \"programming\";\n    \n    return 0;\n}",
//            "3", "Цикл по символам, проверка на a/e/i/o/u",
//            new String[]{null, "3", "true"});
//
//        Lesson l8 = lesson(teacher, 7, "Указатели и структуры",
//            "Адреса, разыменование, struct, typedef.",
//            "<h3>Указатели</h3>" +
//            "<pre><code>int x = 10;\nint *p = &amp;x;\nprintf(\"Значение: %d\\n\", *p);</code></pre>");
//
//        task(l8, 0, "Обмен через указатели", "Напишите функцию <code>swap(int *a, int *b)</code>. Обменяйте a=5, b=10 и выведите.",
//            Difficulty.MEDIUM, 35, "#include <stdio.h>\n\nvoid swap(int *a, int *b) {\n    \n}\n\nint main() {\n    int a=5, b=10;\n    swap(&a, &b);\n    printf(\"%d %d\", a, b);\n    return 0;\n}",
//            "10 5", "int tmp = *a; *a = *b; *b = tmp;",
//            new String[]{null, "10 5", "true"});
//
//        task(l8, 1, "Сумма через указатель", "Функция <code>int sum(int *arr, int n)</code> суммирует массив. Выведите сумму <code>{1,2,3,4,5}</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\nint sum(int *arr, int n) {\n    \n}\n\nint main() {\n    int a[] = {1,2,3,4,5};\n    printf(\"%d\", sum(a, 5));\n    return 0;\n}",
//            "15", "Цикл: total += *(arr + i)",
//            new String[]{null, "15", "true"});
//
//        task(l8, 2, "Структура студент", "Создайте структуру Student (name, grade). Создайте s={\"Alice\", 95} и выведите: <code>Alice 95</code>.",
//            Difficulty.MEDIUM, 30, "#include <stdio.h>\n\n\nint main() {\n    \n    return 0;\n}",
//            "Alice 95", "typedef struct { char name[50]; int grade; } Student;",
//            new String[]{null, "Alice 95", "true"});
//
//        task(l8, 3, "Массив структур", "Массив из 3 студентов. Найдите студента с максимальной оценкой и выведите имя.",
//            Difficulty.HARD, 50, "#include <stdio.h>\n\ntypedef struct { char name[50]; int grade; } Student;\n\nint main() {\n    Student students[] = {{\"Alice\",85},{\"Bob\",92},{\"Carol\",88}};\n    \n    return 0;\n}",
//            "Bob", "Цикл по массиву, сравнение grade с текущим max",
//            new String[]{null, "Bob", "true"});
    }

    @SuppressWarnings("unused")
    private Lesson lesson(User author, int order, String title, String desc, String content) {
        Lesson l = new Lesson();
        l.setTitle(title);
        l.setDescription(desc);
        l.setContent(content);
        l.setOrderIndex(order);
        l.setAuthor(author);
        return lessonRepo.save(l);
    }

    @SuppressWarnings("unused")
    private void task(Lesson lesson, int order, String title, String desc,
                      Difficulty diff, int xp, String template, String expected,
                      String hints, String[]... tests) {
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

        int idx = 0;
        for (String[] tc : tests) {
            TestCase testCase = new TestCase();
            testCase.setTask(t);
            testCase.setInput(tc[0]);
            testCase.setExpectedOutput(tc[1]);
            testCase.setSample("true".equals(tc[2]));
            testCase.setOrderIndex(idx++);
            testCaseRepo.save(testCase);
        }
    }
}

package org.example.config;

import org.example.entity.*;
import org.example.repository.TaskRepository;
import org.example.repository.TestCaseRepository;
import org.example.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final TaskRepository taskRepo;
    private final TestCaseRepository testCaseRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo,
                           TaskRepository taskRepo,
                           TestCaseRepository testCaseRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.testCaseRepo = testCaseRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            User teacher = new User("teacher", "teacher@codequest.dev", encoder.encode("teacher"), Role.TEACHER);
            User student = new User("student", "student@codequest.dev", encoder.encode("student"), Role.STUDENT);
            userRepo.save(teacher);
            userRepo.save(student);
        }
        seedMinimalTaskIfEmpty();
    }

    /**
     * Одна демо-задача для пустой БД — чтобы можно было проверить посылку и Docker без ручного наполнения.
     */
    private void seedMinimalTaskIfEmpty() {
        if (taskRepo.count() > 0) {
            return;
        }
        User teacher = userRepo.findByUsername("teacher").orElse(null);
        if (teacher == null) {
            return;
        }

        Task task = new Task();
        task.setTitle("Hello, CodeQuest (демо)");
        task.setDescription("<p>Выведите ровно текст <code>Hello</code> (без перевода строки).</p>");
        task.setDifficulty(Difficulty.EASY);
        task.setXpReward(10);
        task.setTemplateCode("#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}\n");
        task.setExpectedOutput("Hello");
        task.setHints("Используйте printf(\"Hello\");");
        task.setAuthor(teacher);
        taskRepo.save(task);

        TestCase tc = new TestCase();
        tc.setTask(task);
        tc.setInput("");
        tc.setExpectedOutput("Hello");
        tc.setSample(true);
        tc.setOrderIndex(0);
        testCaseRepo.save(tc);
    }
}

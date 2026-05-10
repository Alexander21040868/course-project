package org.example;

import org.example.config.CodeExecutionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CodeExecutionProperties.class)
public class CodeQuestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeQuestApplication.class, args);
    }
}

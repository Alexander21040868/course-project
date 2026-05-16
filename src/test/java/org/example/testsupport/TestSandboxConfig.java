package org.example.testsupport;

import org.example.config.CodeExecutionProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSandboxConfig {

    @Bean
    @Primary
    public ControllableSandboxDockerService controllableDocker(CodeExecutionProperties props) {
        return new ControllableSandboxDockerService(props);
    }
}

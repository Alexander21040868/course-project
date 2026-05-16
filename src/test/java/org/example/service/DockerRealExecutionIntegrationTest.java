package org.example.service;

import org.example.config.CodeExecutionProperties;
import org.example.testsupport.DockerTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DockerRealExecutionIntegrationTest {

    @BeforeAll
    static void requireDockerDaemon() {
        assumeTrue(DockerTestSupport.isDockerDaemonUp());
    }

    private static CodeExecutionProperties realDockerProps() {
        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(true);
        p.setFallbackLocalGcc(false);
        p.setDockerPath("docker");
        p.setWallTimeoutMs(180_000);
        p.setRunTimeoutSec(10);
        return p;
    }

    @Test
    void docker_compileAndRun_printf_ok() {
        DockerCExecutionService svc = new DockerCExecutionService(realDockerProps());
        CExecutionResult r = svc.compileAndRun(
                "#include <stdio.h>\nint main(void){printf(\"42\\n\");return 0;}\n",
                "");
        assertEquals(CExecutionResult.Kind.OK, r.kind(), stderrDetail(r));
        assertEquals(ExecutionBackend.DOCKER, r.sandboxBackend());
        assertTrue(r.stdout().contains("42"), "stdout=" + r.stdout());
    }

    @Test
    void docker_compileAndRun_stdinEcho_ok() {
        DockerCExecutionService svc = new DockerCExecutionService(realDockerProps());
        String code = """
                #include <stdio.h>
                int main(void) {
                    int x;
                    if (scanf("%d", &x) != 1) return 1;
                    printf("%d", x * 2);
                    return 0;
                }
                """;
        CExecutionResult r = svc.compileAndRun(code, "21\n");
        assertEquals(CExecutionResult.Kind.OK, r.kind(), stderrDetail(r));
        assertEquals(ExecutionBackend.DOCKER, r.sandboxBackend());
        assertTrue(r.stdout().contains("42"), "stdout=" + r.stdout());
    }

    @Test
    void docker_compileAndRun_syntaxError_compileError() {
        DockerCExecutionService svc = new DockerCExecutionService(realDockerProps());
        CExecutionResult r = svc.compileAndRun("this is not valid C !!!\n", "");
        assertEquals(CExecutionResult.Kind.COMPILE_ERROR, r.kind());
        assertEquals(ExecutionBackend.DOCKER, r.sandboxBackend());
        assertFalse(r.stderrOrCompileLog().isBlank());
    }

    @Test
    void docker_compileAndRun_nonZeroExit_runtimeError() {
        DockerCExecutionService svc = new DockerCExecutionService(realDockerProps());
        CExecutionResult r = svc.compileAndRun(
                "#include <stdio.h>\nint main(void){return 9;}\n",
                "");
        assertEquals(CExecutionResult.Kind.RUNTIME_ERROR, r.kind());
        assertEquals(ExecutionBackend.DOCKER, r.sandboxBackend());
    }

    private static String stderrDetail(CExecutionResult r) {
        return "stderr/log: " + r.stderrOrCompileLog();
    }
}

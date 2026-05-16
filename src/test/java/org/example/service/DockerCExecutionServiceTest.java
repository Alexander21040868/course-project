package org.example.service;

import org.example.config.CodeExecutionProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DockerCExecutionServiceTest {

    private static String pathToFalseUtility() {
        for (String candidate : List.of("/usr/bin/false", "/bin/false")) {
            Path p = Path.of(candidate);
            try {
                if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                    return candidate;
                }
            } catch (SecurityException ignored) {
            }
        }
        return null;
    }

    private static boolean gccOnPath() {
        try {
            Process p = new ProcessBuilder("gcc", "--version").start();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void compileAndRun_whenExecutionDisabled_returnsDisabled() {
        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(false);
        DockerCExecutionService svc = new DockerCExecutionService(p);

        CExecutionResult r = svc.compileAndRun("#include <stdio.h>\nint main(){return 0;}\n", "");

        assertEquals(CExecutionResult.Kind.DISABLED, r.kind());
        assertEquals(ExecutionBackend.DISABLED, r.sandboxBackend());
    }

    @Test
    void compileAndRun_whenDockerUnavailableAndNoFallback_returnsDockerError() {
        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(true);
        p.setDockerPath("/nonexistent/cq-docker-" + System.nanoTime());
        p.setFallbackLocalGcc(false);
        p.setWallTimeoutMs(3_000);
        DockerCExecutionService svc = new DockerCExecutionService(p);

        CExecutionResult r = svc.compileAndRun("int main(){return 0;}", "");

        assertEquals(CExecutionResult.Kind.DOCKER_ERROR, r.kind());
        assertEquals(ExecutionBackend.DOCKER_RUN_FAILED, r.sandboxBackend());
        assertFalse(r.stderrOrCompileLog().isBlank());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void compileAndRun_localGccFallback_compileError() {
        assumeTrue(gccOnPath());
        String falseBin = pathToFalseUtility();
        assumeTrue(falseBin != null);

        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(true);
        p.setDockerPath(falseBin);
        p.setFallbackLocalGcc(true);
        p.setRunTimeoutSec(3);
        p.setWallTimeoutMs(5_000);
        DockerCExecutionService svc = new DockerCExecutionService(p);

        CExecutionResult r = svc.compileAndRun("this is not C\n", "");

        assertEquals(CExecutionResult.Kind.COMPILE_ERROR, r.kind());
        assertEquals(ExecutionBackend.LOCAL_GCC, r.sandboxBackend());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void compileAndRun_localGccFallback_successWithStdin() {
        assumeTrue(gccOnPath());
        String falseBin = pathToFalseUtility();
        assumeTrue(falseBin != null);

        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(true);
        p.setDockerPath(falseBin);
        p.setFallbackLocalGcc(true);
        p.setRunTimeoutSec(5);
        p.setWallTimeoutMs(10_000);
        DockerCExecutionService svc = new DockerCExecutionService(p);

        String code = """
                #include <stdio.h>
                int main(void) {
                    int a;
                    if (scanf("%d", &a) != 1) return 1;
                    printf("%d\\n", a * 2);
                    return 0;
                }
                """;

        CExecutionResult r = svc.compileAndRun(code, "21\n");

        assertEquals(CExecutionResult.Kind.OK, r.kind(), r.stderrOrCompileLog());
        assertEquals(ExecutionBackend.LOCAL_GCC, r.sandboxBackend());
        assertTrue(r.stdout().trim().contains("42"), "stdout=" + r.stdout());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void compileAndRun_localGccFallback_runtimeNonZeroExit() {
        assumeTrue(gccOnPath());
        String falseBin = pathToFalseUtility();
        assumeTrue(falseBin != null);

        CodeExecutionProperties p = new CodeExecutionProperties();
        p.setEnabled(true);
        p.setDockerPath(falseBin);
        p.setFallbackLocalGcc(true);
        p.setRunTimeoutSec(5);
        p.setWallTimeoutMs(10_000);
        DockerCExecutionService svc = new DockerCExecutionService(p);

        String code = """
                #include <stdio.h>
                int main(void) { return 7; }
                """;

        CExecutionResult r = svc.compileAndRun(code, "");

        assertEquals(CExecutionResult.Kind.RUNTIME_ERROR, r.kind());
        assertEquals(ExecutionBackend.LOCAL_GCC, r.sandboxBackend());
    }
}

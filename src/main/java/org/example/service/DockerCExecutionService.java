package org.example.service;

import org.example.config.CodeExecutionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerCExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DockerCExecutionService.class);

    private final CodeExecutionProperties props;

    public DockerCExecutionService(CodeExecutionProperties props) {
        this.props = props;
    }

    public CExecutionResult compileAndRun(String source, String stdin) {
        if (!props.isEnabled()) {
            return CExecutionResult.disabled();
        }

        String code = source == null ? "" : source;
        String in = stdin == null ? "" : stdin;

        Path work = null;
        try {
            work = Files.createTempDirectory("cq-c-");
            Path solution = work.resolve("solution.c");
            Path input = work.resolve("input.txt");
            Path runner = work.resolve("run.sh");

            Files.writeString(solution, code, StandardCharsets.UTF_8);
            Files.writeString(input, in, StandardCharsets.UTF_8);
            Files.writeString(runner, buildRunnerScript(props.getRunTimeoutSec()), StandardCharsets.UTF_8);

            boolean dockerRan = tryDockerRun(work);
            if (!dockerRan && props.isFallbackLocalGcc()) {
                log.info("Docker недоступен или завершился с ошибкой — проверка через локальный gcc.");
                scrubArtifacts(work);
                return runLocalGcc(work, in);
            }
            if (!dockerRan) {
                return CExecutionResult.dockerError(
                        "Не удалось запустить Docker. Запустите Docker Desktop или включите app.code-execution.fallback-local-gcc.");
            }

            return readWorkdirResults(work);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("C sandbox error", e);
            if (props.isFallbackLocalGcc() && work != null) {
                try {
                    scrubArtifacts(work);
                    return runLocalGcc(work, in);
                } catch (IOException | InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            return CExecutionResult.dockerError(e.getMessage());
        } finally {
            if (work != null) {
                try {
                    deleteRecursive(work);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean tryDockerRun(Path work) throws IOException, InterruptedException {
        List<String> cmd = List.of(
                props.getDockerPath(),
                "run", "--rm",
                "--network", "none",
                "--memory", "128m",
                "--cpus", "0.5",
                "--pids-limit", "64",
                "--security-opt", "no-new-privileges",
                "-v", work.toAbsolutePath() + ":/work:rw",
                props.getDockerImage(),
                "sh", "/work/run.sh"
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String dockerOut = readAll(p.getInputStream());

        boolean finished = p.waitFor(props.getWallTimeoutMs(), TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            log.warn("docker: timeout, output head: {}", truncate(dockerOut, 200));
            return false;
        }
        if (p.exitValue() != 0) {
            log.warn("docker exited {}: {}", p.exitValue(), truncate(dockerOut, 400));
            return false;
        }
        return true;
    }

    private static void scrubArtifacts(Path work) throws IOException {
        for (String name : List.of(
                "prog", "prog.exe", "compile.log", "stdout.txt", "stderr.txt", "exitcode.txt",
                "compile_failed.marker", "timed_out.marker")) {
            Files.deleteIfExists(work.resolve(name));
        }
    }

    private static String buildRunnerScript(int runTimeoutSec) {
        return """
                #!/bin/sh
                set -e
                cd "$(dirname "$0")"
                rm -f compile.log stdout.txt stderr.txt exitcode.txt compile_failed.marker timed_out.marker prog
                if ! gcc -std=c11 -pipe -O0 -o prog solution.c 2>compile.log; then
                  touch compile_failed.marker
                  exit 0
                fi
                set +e
                _r=
                if command -v stdbuf >/dev/null 2>&1; then
                  _r="stdbuf -o0 "
                fi
                cat input.txt | timeout -k 1 %ds ${_r}./prog >stdout.txt 2>stderr.txt
                ec=$?
                echo "$ec" > exitcode.txt
                case "$ec" in
                  124|137|143) touch timed_out.marker ;;
                esac
                exit 0
                """.formatted(runTimeoutSec);
    }

    private CExecutionResult runLocalGcc(Path work, String stdin) throws IOException, InterruptedException {
        String exe = localExeFile();
        ProcessBuilder gccPb = new ProcessBuilder(
                "gcc", "-std=c11", "-pipe", "-O0", "-o", exe, "solution.c");
        gccPb.directory(work.toFile());
        gccPb.redirectErrorStream(true);
        Process gccProc = gccPb.start();
        String compileLog = readAll(gccProc.getInputStream());
        if (!gccProc.waitFor(25, TimeUnit.SECONDS)) {
            gccProc.destroyForcibly();
            return new CExecutionResult(CExecutionResult.Kind.TIMEOUT, "", "Компиляция не уложилась во время.");
        }
        if (gccProc.exitValue() != 0) {
            return new CExecutionResult(CExecutionResult.Kind.COMPILE_ERROR, "", compileLog);
        }

        Path binary = work.resolve(exe);
        ProcessBuilder runPb = new ProcessBuilder(binary.toAbsolutePath().toString());
        runPb.directory(work.toFile());
        Process runProc = runPb.start();
        try (OutputStream os = runProc.getOutputStream()) {
            os.write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        long runMs = Math.max(1_000L, props.getRunTimeoutSec() * 1000L + 2_000L);
        boolean done = runProc.waitFor(runMs, TimeUnit.MILLISECONDS);
        if (!done) {
            runProc.destroyForcibly();
            return new CExecutionResult(CExecutionResult.Kind.TIMEOUT, "", "Программа не уложилась в лимит времени.");
        }
        String out = readAll(runProc.getInputStream());
        String err = readAll(runProc.getErrorStream());
        int code = runProc.exitValue();
        if (code != 0) {
            String detail = err.isBlank() ? ("код выхода " + code) : err;
            return new CExecutionResult(CExecutionResult.Kind.RUNTIME_ERROR, out, detail);
        }
        return new CExecutionResult(CExecutionResult.Kind.OK, out, err);
    }

    private static String localExeFile() {
        return isWindows() ? "prog.exe" : "prog";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static CExecutionResult readWorkdirResults(Path work) throws IOException {
        Path marker = work.resolve("compile_failed.marker");
        if (Files.exists(marker)) {
            String logText = readIfExists(work.resolve("compile.log"));
            return new CExecutionResult(CExecutionResult.Kind.COMPILE_ERROR, "", logText);
        }
        if (Files.exists(work.resolve("timed_out.marker"))) {
            return new CExecutionResult(CExecutionResult.Kind.TIMEOUT, "", "Программа не уложилась в лимит времени.");
        }

        String out = readIfExists(work.resolve("stdout.txt"));
        String err = readIfExists(work.resolve("stderr.txt"));
        String exitRaw = readIfExists(work.resolve("exitcode.txt")).trim();
        int exit = 0;
        try {
            if (!exitRaw.isEmpty()) {
                exit = Integer.parseInt(exitRaw.split("\\s")[0]);
            }
        } catch (NumberFormatException ignored) {
        }

        if (exit != 0) {
            String detail = err.isBlank() ? ("код выхода " + exit) : err;
            return new CExecutionResult(CExecutionResult.Kind.RUNTIME_ERROR, out, detail);
        }
        return new CExecutionResult(CExecutionResult.Kind.OK, out, err);
    }

    private static String readIfExists(Path p) throws IOException {
        if (!Files.exists(p)) {
            return "";
        }
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static String readAll(java.io.InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            List<Path> paths = new ArrayList<>();
            walk.forEach(paths::add);
            paths.sort((a, b) -> b.getNameCount() - a.getNameCount());
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}

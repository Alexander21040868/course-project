package org.example.testsupport;

import org.example.config.CodeExecutionProperties;
import org.example.service.CExecutionResult;
import org.example.service.DockerCExecutionService;
import org.example.service.ExecutionBackend;

import java.util.function.BiFunction;

public class ControllableSandboxDockerService extends DockerCExecutionService {

    private volatile BiFunction<String, String, CExecutionResult> handler =
            (code, stdin) -> new CExecutionResult(
                    CExecutionResult.Kind.OK, "3\n", "", ExecutionBackend.LOCAL_GCC);

    public ControllableSandboxDockerService(CodeExecutionProperties props) {
        super(props);
    }

    public void setHandler(BiFunction<String, String, CExecutionResult> handler) {
        this.handler = handler != null ? handler : (c, i) -> new CExecutionResult(
                CExecutionResult.Kind.OK, "3\n", "", ExecutionBackend.LOCAL_GCC);
    }

    @Override
    public CExecutionResult compileAndRun(String source, String stdin) {
        return handler.apply(source, stdin);
    }
}

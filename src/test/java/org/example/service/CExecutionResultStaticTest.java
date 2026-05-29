package org.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CExecutionResultStaticTest {

    @Test
    void disabled_containsMessageAndBackend() {
        CExecutionResult r = CExecutionResult.disabled();
        assertEquals(CExecutionResult.Kind.DISABLED, r.kind());
        assertEquals(ExecutionBackend.DISABLED, r.sandboxBackend());
        assertFalse(r.stderrOrCompileLog().isBlank());
    }

    @Test
    void dockerError_usesDefaultWhenNullMessage() {
        CExecutionResult r = CExecutionResult.dockerError(null);
        assertEquals(CExecutionResult.Kind.DOCKER_ERROR, r.kind());
        assertTrue(r.stderrOrCompileLog().toLowerCase().contains("docker"));
    }
}

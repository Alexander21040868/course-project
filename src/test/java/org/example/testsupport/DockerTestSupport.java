package org.example.testsupport;

import java.util.concurrent.TimeUnit;

public final class DockerTestSupport {

    private DockerTestSupport() {
    }

    public static boolean isDockerDaemonUp() {
        try {
            Process p = new ProcessBuilder("docker", "info")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

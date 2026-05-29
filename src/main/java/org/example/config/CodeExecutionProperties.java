package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.code-execution")
public class CodeExecutionProperties {

    private boolean enabled = true;
    private String dockerImage = "public.ecr.aws/docker/library/gcc:14-bookworm";
    private String dockerPath = "docker";
    private long wallTimeoutMs = 45_000;
    private int runTimeoutSec = 5;
    private boolean fallbackLocalGcc = true;

    private String dockerSandboxVolume;

    private String dockerSandboxMountPath = "/cq-sandbox";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getDockerPath() {
        return dockerPath;
    }

    public void setDockerPath(String dockerPath) {
        this.dockerPath = dockerPath;
    }

    public long getWallTimeoutMs() {
        return wallTimeoutMs;
    }

    public void setWallTimeoutMs(long wallTimeoutMs) {
        this.wallTimeoutMs = wallTimeoutMs;
    }

    public int getRunTimeoutSec() {
        return runTimeoutSec;
    }

    public void setRunTimeoutSec(int runTimeoutSec) {
        this.runTimeoutSec = runTimeoutSec;
    }

    public boolean isFallbackLocalGcc() {
        return fallbackLocalGcc;
    }

    public void setFallbackLocalGcc(boolean fallbackLocalGcc) {
        this.fallbackLocalGcc = fallbackLocalGcc;
    }

    public String getDockerSandboxVolume() {
        return dockerSandboxVolume;
    }

    public void setDockerSandboxVolume(String dockerSandboxVolume) {
        this.dockerSandboxVolume = dockerSandboxVolume;
    }

    public String getDockerSandboxMountPath() {
        return dockerSandboxMountPath;
    }

    public void setDockerSandboxMountPath(String dockerSandboxMountPath) {
        this.dockerSandboxMountPath = dockerSandboxMountPath;
    }
}

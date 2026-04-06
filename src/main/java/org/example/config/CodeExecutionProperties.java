package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.code-execution")
public class CodeExecutionProperties {

    /** Если false — отправки не компилируются (удобно для тестов без Docker). */
    private boolean enabled = true;
    /** Debian + GNU coreutils (корректный `timeout`); можно заменить на свой образ с gcc. */
    private String dockerImage = "gcc:14-bookworm";
    private String dockerPath = "docker";
    /** Общий лимит ожидания docker-процесса (компиляция + запуск). */
    private long wallTimeoutMs = 45_000;
    private int runTimeoutSec = 5;

    /**
     * Если docker недоступен — компиляция и запуск через локальный gcc в temp (удобно для разработки).
     * На проде с изоляцией лучше выключить.
     */
    private boolean fallbackLocalGcc = true;

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
}

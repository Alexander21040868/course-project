package org.example.support;

import java.util.UUID;

public final class TestId {

    private TestId() {}

    public static String uniq(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

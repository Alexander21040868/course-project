package org.example.util;

public final class XpLimits {

    public static final int TASK_MIN = 1;
    public static final int TASK_MAX = 300;

    public static final int CHALLENGE_BONUS_MIN = 1;
    public static final int CHALLENGE_BONUS_MAX = 2000;

    private XpLimits() {}

    public static int normalizeTaskXp(int raw) {
        int v = raw <= 0 ? 10 : raw;
        if (v < TASK_MIN || v > TASK_MAX) {
            throw new IllegalArgumentException("Награда XP за задачу допустима от " + TASK_MIN + " до " + TASK_MAX);
        }
        return v;
    }

    public static int normalizeChallengeBonusXp(int raw) {
        if (raw <= 0) return 50;
        if (raw < CHALLENGE_BONUS_MIN || raw > CHALLENGE_BONUS_MAX) {
            throw new IllegalArgumentException("Бонус XP соревнования: от " + CHALLENGE_BONUS_MIN + " до " + CHALLENGE_BONUS_MAX + ", либо 0 для значения по умолчанию (50)");
        }
        return raw;
    }
}

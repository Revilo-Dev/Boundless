package net.revilodev.boundless;

import java.util.concurrent.ConcurrentHashMap;

/** Lightweight, rate-limited diagnostics for investigating live server performance. */
public final class BoundlessDebug {
    private static final ConcurrentHashMap<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();

    private BoundlessDebug() {}

    public static boolean enabled() {
        return Config.devMode();
    }

    /** Log at most once per interval for a diagnostic category. */
    public static void rateLimited(String category, long intervalMillis, String message, Object... arguments) {
        if (!enabled()) return;
        long now = System.nanoTime();
        long intervalNanos = intervalMillis * 1_000_000L;
        Long previous = LAST_LOG_NANOS.putIfAbsent(category, now);
        if (previous != null && now - previous < intervalNanos) return;
        LAST_LOG_NANOS.put(category, now);
        BoundlessMod.LOGGER.info("[Boundless/Dev][{}] " + message, prepend(category, arguments));
    }

    private static Object[] prepend(String category, Object[] arguments) {
        Object[] values = new Object[arguments.length + 1];
        values[0] = category;
        System.arraycopy(arguments, 0, values, 1, arguments.length);
        return values;
    }
}

package net.revilodev.boundless;

import java.util.concurrent.ConcurrentHashMap;

// rate-limited diagnostics for testing server performance
public final class BoundlessDebug {
    private static final ConcurrentHashMap<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();

    private BoundlessDebug() {}

    public static boolean enabled() {
        return Config.devMode();
    }

    //Log once per int per diagnostic
    public static void rateLimited(String category, long intervalMillis, String message, Object... arguments) {
        if (!enabled() || !categoryEnabled(category)) return;
        if (!takeLogSlot(category, intervalMillis)) return;
        BoundlessMod.LOGGER.info("[Boundless/Dev][{}] " + message, prepend(category, arguments));
    }

    private static boolean categoryEnabled(String category) {
        String normalized = category == null ? "" : category.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("panel") || normalized.contains("render") || normalized.contains("editor")) {
            return Config.debugUiPerformance();
        }
        if (normalized.contains("upload") || normalized.contains("download") || normalized.contains("transfer")
                || normalized.contains("sync") || normalized.contains("network")) {
            return Config.debugNetworkTransfers();
        }
        return Config.debugQuestLoading();
    }

    // this keeps repeated failures visible without filling the log
    public static void warnRateLimited(String category, long intervalMillis, String message, Object... arguments) {
        if (!takeLogSlot("warn:" + category, intervalMillis)) return;
        BoundlessMod.LOGGER.warn("[Boundless][{}] " + message, prepend(category, arguments));
    }

    private static boolean takeLogSlot(String category, long intervalMillis) {
        long now = System.nanoTime();
        long intervalNanos = intervalMillis * 1_000_000L;
        Long previous = LAST_LOG_NANOS.putIfAbsent(category, now);
        if (previous != null && now - previous < intervalNanos) return false;
        LAST_LOG_NANOS.put(category, now);
        return true;
    }

    private static Object[] prepend(String category, Object[] arguments) {
        Object[] values = new Object[arguments.length + 1];
        values[0] = category;
        System.arraycopy(arguments, 0, values, 1, arguments.length);
        return values;
    }
}

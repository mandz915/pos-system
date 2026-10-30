package com.pos.gateway.filter;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-key fixed one-minute window counter. In memory and per gateway instance: good protection against a runaway
 * client or a login brute force, but not an exact cluster-wide quota (use Redis for that).
 */
public class FixedWindowRateLimiter {

    private record Window(long minute, int count) {
    }

    private final int limitPerMinute;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMinute = new AtomicLong(-1);

    public FixedWindowRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    /** @return true when the request is allowed, false when the key exceeded its limit in the current minute */
    public boolean allow(String key) {
        long minute = clock.millis() / 60_000;
        cleanup(minute);
        Window window = windows.compute(key, (k, old) ->
                old == null || old.minute() != minute ? new Window(minute, 1) : new Window(minute, old.count() + 1));
        return window.count() <= limitPerMinute;
    }

    /** Seconds until the current window ends. */
    public long retryAfterSeconds() {
        return 60 - (clock.millis() / 1000) % 60;
    }

    private void cleanup(long minute) {
        long previous = lastCleanupMinute.get();
        if (previous != minute && lastCleanupMinute.compareAndSet(previous, minute)) {
            windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        }
    }
}

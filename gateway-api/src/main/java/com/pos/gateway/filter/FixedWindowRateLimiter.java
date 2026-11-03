package com.pos.gateway.filter;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    public boolean allow(String key) {
        long minute = clock.millis() / 60_000;
        cleanup(minute);
        Window window = windows.compute(key, (k, old) ->
                old == null || old.minute() != minute ? new Window(minute, 1) : new Window(minute, old.count() + 1));
        return window.count() <= limitPerMinute;
    }

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

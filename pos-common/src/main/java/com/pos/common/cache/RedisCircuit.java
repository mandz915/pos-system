package com.pos.common.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public class RedisCircuit {

    private final int threshold;
    private final long openMillis;
    private final LongSupplier clock;
    private final AtomicInteger failures = new AtomicInteger();
    private volatile long openUntil;

    public RedisCircuit(int threshold, long openMillis) {
        this(threshold, openMillis, System::currentTimeMillis);
    }

    public RedisCircuit(int threshold, long openMillis, LongSupplier clock) {
        this.threshold = threshold;
        this.openMillis = openMillis;
        this.clock = clock;
    }

    public boolean allow() {
        return clock.getAsLong() >= openUntil;
    }

    public void success() {
        failures.set(0);
    }

    public void failure() {
        if (failures.incrementAndGet() >= threshold) {
            openUntil = clock.getAsLong() + openMillis;
            failures.set(0);
        }
    }
}

package com.pos.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsUpToTheLimitThenBlocks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:10Z"));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, clock);

        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isFalse();
    }

    @Test
    void keysAreIndependent() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:10Z"));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, clock);

        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isFalse();
        assertThat(limiter.allow("b")).isTrue();
    }

    @Test
    void counterResetsInTheNextMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:10Z"));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, clock);

        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isFalse();
        clock.advanceSeconds(60);
        assertThat(limiter.allow("a")).isTrue();
    }

    @Test
    void retryAfterIsTheRemainderOfTheMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:45Z"));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, clock);

        assertThat(limiter.retryAfterSeconds()).isEqualTo(15);
    }
}

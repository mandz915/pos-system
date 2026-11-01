package com.pos.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RedisCircuitTest {

    @Test
    void opensAfterConsecutiveFailuresAndClosesAfterTheWindow() {
        AtomicLong now = new AtomicLong(1_000);
        RedisCircuit circuit = new RedisCircuit(3, 5_000, now::get);

        circuit.failure();
        circuit.failure();
        assertThat(circuit.allow()).isTrue();
        circuit.failure();
        assertThat(circuit.allow()).isFalse();

        now.addAndGet(4_999);
        assertThat(circuit.allow()).isFalse();
        now.addAndGet(1);
        assertThat(circuit.allow()).isTrue();
    }

    @Test
    void aSuccessResetsTheFailureCount() {
        RedisCircuit circuit = new RedisCircuit(2, 5_000, () -> 0L);

        circuit.failure();
        circuit.success();
        circuit.failure();

        assertThat(circuit.allow()).isTrue();
    }
}

package com.pos.gateway.filter;

import java.time.Clock;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.pos.common.cache.RedisCircuit;

import reactor.core.publisher.Mono;

@Component
public class RedisRateLimiter {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    private final ReactiveStringRedisTemplate redis;
    private final RedisCircuit circuit = new RedisCircuit(5, 5_000);
    private final Clock clock = Clock.systemUTC();

    public RedisRateLimiter(ObjectProvider<ReactiveStringRedisTemplate> redis) {
        this.redis = redis.getIfAvailable();
    }

    public boolean available() {
        return redis != null;
    }

    public Mono<Boolean> allow(String key, int limitPerMinute, BooleanSupplier fallback) {
        if (redis == null || !circuit.allow()) {
            return Mono.fromSupplier(fallback::getAsBoolean);
        }
        String redisKey = "pos:rl:" + key + ":" + clock.millis() / 60_000;
        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> count == 1
                        ? redis.expire(redisKey, Duration.ofSeconds(120)).thenReturn(count)
                        : Mono.just(count))
                .map(count -> count <= limitPerMinute)
                .timeout(TIMEOUT)
                .doOnNext(ok -> circuit.success())
                .onErrorResume(e -> {
                    circuit.failure();
                    return Mono.fromSupplier(fallback::getAsBoolean);
                });
    }
}

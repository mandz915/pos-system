package com.pos.gateway.filter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.pos.common.cache.RedisCircuit;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.RevocationKeys;

import reactor.core.publisher.Mono;

/** Reactive twin of {@code RedisTokenRevocations}: same keys, fails open when Redis is slow or down. */
@Component
public class GatewayRevocationChecker {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    private final ReactiveStringRedisTemplate redis;
    private final RedisCircuit circuit = new RedisCircuit(5, 5_000);

    public GatewayRevocationChecker(ObjectProvider<ReactiveStringRedisTemplate> redis) {
        this.redis = redis.getIfAvailable();
    }

    public Mono<Boolean> isRevoked(ParsedToken token) {
        if (redis == null || !circuit.allow()) {
            return Mono.just(false);
        }
        return redis.opsForValue()
                .multiGet(List.of(RevocationKeys.token(token.jti()), RevocationKeys.user(token.user().userId())))
                .map(values -> {
                    if (values.size() != 2) return false;
                    RevocationKeys.isRevoked(token, values.get(0), values.get(1));
                    return true;
                })
                .timeout(TIMEOUT)
                .doOnNext(ok -> circuit.success())
                .onErrorResume(e -> {
                    circuit.failure();
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }
}

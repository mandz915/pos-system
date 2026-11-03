package com.pos.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.pos.common.security.ParsedToken;
import com.pos.common.security.RevocationKeys;
import com.pos.common.security.TokenRevocations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RedisTokenRevocations implements TokenRevocations {

    private final StringRedisTemplate redis;
    private final RedisCircuit circuit;
    private final Duration tokenTtl;

    @Override
    public boolean isRevoked(ParsedToken token) {
        if (!circuit.allow()) {
            return false;
        }
        try {
            List<String> values = redis.opsForValue().multiGet(
                    List.of(RevocationKeys.token(token.jti()), RevocationKeys.user(token.user().userId())));
            circuit.success();
            return values != null && values.size() == 2 && RevocationKeys.isRevoked(token, values.get(0), values.get(1));
        } catch (RuntimeException e) {
            circuit.failure();
            log.warn("Revocation check skipped, Redis unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void revokeToken(ParsedToken token) {
        Duration remaining = Duration.between(Instant.now(), token.expiresAt());
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        write(RevocationKeys.token(token.jti()), "1", remaining);
    }

    @Override
    public void revokeUser(long userId) {
        write(RevocationKeys.user(userId), String.valueOf(System.currentTimeMillis()), tokenTtl);
    }

    private void write(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
            circuit.success();
        } catch (RuntimeException e) {
            circuit.failure();
            log.error("Could not record token revocation ({}): {}. Affected tokens stay valid until they expire.",
                    key, e.getMessage());
        }
    }
}

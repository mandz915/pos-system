package com.pos.gateway.filter;

import java.time.Clock;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.pos.gateway.config.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * Throttles requests after authentication: signed-in traffic is limited per USER (a whole store of tills sharing one
 * public IP must not be throttled as one client), anonymous traffic (login) per client address, with a much lower
 * limit so password guessing stays expensive. Counters live in Redis (cluster-wide) with an in-memory fallback.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final int perUser;
    private final int perAddress;
    private final RedisRateLimiter redisLimiter;
    private final FixedWindowRateLimiter userFallback;
    private final FixedWindowRateLimiter addressFallback;

    public RateLimitFilter(GatewayProperties properties, RedisRateLimiter redisLimiter) {
        GatewayProperties.RateLimit limits = properties.rateLimit();
        this.enabled = limits.enabled();
        this.trustForwardedFor = limits.trustForwardedFor();
        this.perUser = limits.requestsPerMinute();
        this.perAddress = limits.anonymousPerMinute();
        this.redisLimiter = redisLimiter;
        this.userFallback = new FixedWindowRateLimiter(perUser, Clock.systemUTC());
        this.addressFallback = new FixedWindowRateLimiter(perAddress, Clock.systemUTC());
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }
        Long userId = exchange.getAttribute(JwtAuthFilter.USER_ID_ATTRIBUTE);
        String key;
        int limit;
        FixedWindowRateLimiter fallback;
        if (userId != null) {
            key = "u:" + userId;
            limit = perUser;
            fallback = userFallback;
        } else {
            key = "ip:" + clientAddress(exchange.getRequest());
            limit = perAddress;
            fallback = addressFallback;
        }
        String fallbackKey = key;
        return redisLimiter.allow(key, limit, () -> fallback.allow(fallbackKey)).flatMap(allowed -> {
            if (allowed) {
                return chain.filter(exchange);
            }
            exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(fallback.retryAfterSeconds()));
            return JsonErrors.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many requests, slow down");
        });
    }

    private String clientAddress(ServerHttpRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}

package com.pos.gateway.filter;

import java.time.Clock;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.pos.gateway.config.GatewayProperties;

import reactor.core.publisher.Mono;

/** Throttles requests per client address before anything else runs (so login brute force is limited too). */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final FixedWindowRateLimiter limiter;

    public RateLimitFilter(GatewayProperties properties) {
        this.enabled = properties.rateLimit().enabled();
        this.trustForwardedFor = properties.rateLimit().trustForwardedFor();
        this.limiter = new FixedWindowRateLimiter(properties.rateLimit().requestsPerMinute(), Clock.systemUTC());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled || limiter.allow(clientKey(exchange.getRequest()))) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(limiter.retryAfterSeconds()));
        return JsonErrors.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many requests, slow down");
    }

    private String clientKey(ServerHttpRequest request) {
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
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

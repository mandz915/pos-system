package com.pos.gateway.filter;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.pos.common.security.InvalidTokenException;
import com.pos.common.security.JwtService;
import com.pos.common.security.ParsedToken;
import com.pos.gateway.config.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * Rejects requests without a valid, non-revoked JWT at the edge. The token is forwarded unchanged: every service
 * validates it again (defence in depth) and reads roles and store from it.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /** Exchange attribute holding the id of the authenticated user (Long), used by the rate limiter. */
    public static final String USER_ID_ATTRIBUTE = "pos.userId";

    private final JwtService jwtService;
    private final PublicPaths publicPaths;
    private final GatewayRevocationChecker revocations;

    public JwtAuthFilter(JwtService jwtService, GatewayProperties properties, GatewayRevocationChecker revocations) {
        this.jwtService = jwtService;
        this.publicPaths = new PublicPaths(properties.publicPaths());
        this.revocations = revocations;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // CORS pre-flight requests carry no credentials by design.
        if (HttpMethod.OPTIONS.equals(request.getMethod()) || publicPaths.matches(request.getURI().getPath())) {
            return chain.filter(exchange);
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return JsonErrors.write(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        ParsedToken parsed;
        try {
            parsed = jwtService.parseDetailed(header.substring(7).trim());
        } catch (InvalidTokenException e) {
            return JsonErrors.write(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
        }
        return revocations.isRevoked(parsed).flatMap(revoked -> {
            if (revoked) {
                return JsonErrors.write(exchange, HttpStatus.UNAUTHORIZED, "Token has been revoked, please sign in again");
            }
            exchange.getAttributes().put(USER_ID_ATTRIBUTE, parsed.user().userId());
            return chain.filter(exchange);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

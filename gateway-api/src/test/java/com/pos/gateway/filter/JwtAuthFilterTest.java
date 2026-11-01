package com.pos.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.JwtProperties;
import com.pos.common.security.JwtService;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.Roles;
import com.pos.gateway.config.GatewayProperties;
import com.pos.gateway.config.GatewayProperties.RateLimit;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    private final JwtService jwt = new JwtService(
            new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ofHours(1), "pos-system"));

    @Mock
    private GatewayRevocationChecker revocations;
    @Mock
    private GatewayFilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties("*", "/api/auth/login,/api/auth/pin-login",
                new RateLimit(true, 1200, 60, false), null);
        filter = new JwtAuthFilter(jwt, properties, revocations);
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(revocations.isRevoked(any(ParsedToken.class))).thenReturn(Mono.just(false));
    }

    private String token() {
        return jwt.issue(new AuthenticatedUser(10L, "cora", Set.of(Roles.CASHIER), 1L, 1L));
    }

    private MockServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get(path);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(request);
    }

    @Test
    void aValidTokenPassesAndTheUserIdIsHandedToTheRateLimiter() {
        MockServerWebExchange exchange = exchange("/api/sales", "Bearer " + token());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat((Long) exchange.getAttribute(JwtAuthFilter.USER_ID_ATTRIBUTE)).isEqualTo(10L);
    }

    @Test
    void aMissingTokenIs401AndNothingIsForwarded() {
        MockServerWebExchange exchange = exchange("/api/sales", null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void aGarbageTokenIs401() {
        MockServerWebExchange exchange = exchange("/api/sales", "Bearer not-a-jwt");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void aRevokedTokenIs401EvenThoughItIsStillSigned() {
        when(revocations.isRevoked(any(ParsedToken.class))).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = exchange("/api/sales", "Bearer " + token());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void loginIsPublicAndNeverChecksRevocation() {
        MockServerWebExchange exchange = exchange("/api/auth/login", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(revocations, never()).isRevoked(any());
    }
}

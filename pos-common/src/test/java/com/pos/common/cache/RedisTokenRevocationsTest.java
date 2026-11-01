package com.pos.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.Roles;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisTokenRevocationsTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> ops;

    private RedisTokenRevocations revocations;
    private ParsedToken token;

    @BeforeEach
    void setUp() {
        revocations = new RedisTokenRevocations(redis, new RedisCircuit(3, 60_000), Duration.ofHours(8));
        when(redis.opsForValue()).thenReturn(ops);
        Instant now = Instant.now();
        token = new ParsedToken(new AuthenticatedUser(7L, "cora", Set.of(Roles.CASHIER), 1L, 1L), "jti-1",
                now.minusSeconds(60), now.plusSeconds(3600));
    }

    @Test
    void aCleanTokenIsNotRevoked() {
        when(ops.multiGet(List.of("pos:rev:jti:jti-1", "pos:rev:user:7"))).thenReturn(Arrays.asList(null, null));

        assertThat(revocations.isRevoked(token)).isFalse();
    }

    @Test
    void aLoggedOutTokenIsRevoked() {
        when(ops.multiGet(List.of("pos:rev:jti:jti-1", "pos:rev:user:7"))).thenReturn(Arrays.asList("1", null));

        assertThat(revocations.isRevoked(token)).isTrue();
    }

    @Test
    void aTokenIssuedBeforeTheUsersRevocationIsRevoked() {
        String revokedAt = String.valueOf(System.currentTimeMillis());
        when(ops.multiGet(List.of("pos:rev:jti:jti-1", "pos:rev:user:7"))).thenReturn(Arrays.asList(null, revokedAt));

        assertThat(revocations.isRevoked(token)).isTrue();
    }

    @Test
    void whenRedisIsDownRequestsAreStillServed() {
        when(ops.multiGet(any())).thenThrow(new IllegalStateException("connection refused"));

        assertThat(revocations.isRevoked(token)).isFalse();
    }

    @Test
    void revokingATokenKeepsItUntilItWouldHaveExpiredAnyway() {
        revocations.revokeToken(token);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(ops).set(eq("pos:rev:jti:jti-1"), eq("1"), ttl.capture());
        assertThat(ttl.getValue()).isBetween(Duration.ofSeconds(3500), Duration.ofSeconds(3600));
    }

    @Test
    void anAlreadyExpiredTokenNeedsNoBlocklistEntry() {
        ParsedToken expired = new ParsedToken(token.user(), "old", Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600));

        revocations.revokeToken(expired);

        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void revokingAUserLastsAsLongAsAnyTokenCouldLive() {
        revocations.revokeUser(7L);

        verify(ops).set(eq("pos:rev:user:7"), anyString(), eq(Duration.ofHours(8)));
    }
}

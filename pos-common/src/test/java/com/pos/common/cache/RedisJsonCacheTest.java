package com.pos.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.pos.common.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisJsonCacheTest {

    public record Item(String name, BigDecimal price) {
    }

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> ops;

    private RedisCircuit circuit;
    private RedisJsonCache cache;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
        circuit = new RedisCircuit(2, 60_000);
        cache = new RedisJsonCache(redis, circuit);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("pos:cache:ver:catalog:t1")).thenReturn("3");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void everyTenantHasItsOwnCopyOfTheNamespace() {
        when(ops.get("pos:cache:ver:catalog:t2")).thenReturn("0");
        when(ops.get("pos:cache:catalog:t1:3:product:5")).thenReturn("{\"name\":\"Coffee\",\"price\":1}");

        assertThat(cache.get("catalog", "product:5", Item.class)).isPresent();

        TenantContext.set(2L);
        assertThat(cache.get("catalog", "product:5", Item.class)).isEmpty();
        verify(ops).get("pos:cache:catalog:t2:0:product:5");
    }

    @Test
    void evictingOneTenantLeavesTheOthersCached() {
        when(ops.increment("pos:cache:ver:catalog:t1")).thenReturn(4L);

        cache.evictAll("catalog");

        verify(ops, never()).increment("pos:cache:ver:catalog:t2");
    }

    @Test
    void withoutATenantNothingIsReadWrittenOrEvicted() {
        TenantContext.clear();

        assertThat(cache.get("catalog", "product:5", Item.class)).isEmpty();
        cache.put("catalog", "product:5", new Item("x", BigDecimal.ONE), Duration.ofSeconds(1));
        cache.evictAll("catalog");

        verify(ops, never()).get(startsWith("pos:cache:catalog"));
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
        verify(ops, never()).increment(anyString());
    }

    @Test
    void entriesLiveUnderTheNamespaceVersion() {
        when(ops.get("pos:cache:catalog:t1:3:product:5")).thenReturn("{\"name\":\"Coffee\",\"price\":120.00,\"extra\":1}");

        Optional<Item> hit = cache.get("catalog", "product:5", Item.class);

        assertThat(hit).isPresent();
        assertThat(hit.get().name()).isEqualTo("Coffee");
        assertThat(hit.get().price()).isEqualByComparingTo("120.00");
    }

    @Test
    void aMissIsEmpty() {
        assertThat(cache.get("catalog", "product:404", Item.class)).isEmpty();
    }

    @Test
    void anUnreadableEntryIsTreatedAsAMiss() {
        when(ops.get("pos:cache:catalog:t1:3:product:5")).thenReturn("not json");

        assertThat(cache.get("catalog", "product:5", Item.class)).isEmpty();
    }

    @Test
    void putStoresJsonWithTheTtl() {
        cache.put("catalog", "product:5", new Item("Coffee", new BigDecimal("120.00")), Duration.ofSeconds(30));

        verify(ops).set(eq("pos:cache:catalog:t1:3:product:5"), anyString(), eq(Duration.ofSeconds(30)));
    }

    @Test
    void evictingBumpsTheVersionSoOlderEntriesAreNoLongerRead() {
        when(ops.increment("pos:cache:ver:catalog:t1")).thenReturn(4L);

        cache.evictAll("catalog");
        cache.get("catalog", "product:5", Item.class);

        verify(ops).get("pos:cache:catalog:t1:4:product:5");
        verify(ops, never()).get("pos:cache:catalog:t1:3:product:5");
    }

    @Test
    void redisFailuresBecomeMissesAndEventuallyStopCallingRedis() {
        when(ops.get(anyString())).thenThrow(new IllegalStateException("connection refused"));

        assertThat(cache.get("catalog", "a", Item.class)).isEmpty();
        assertThat(cache.get("catalog", "b", Item.class)).isEmpty();
        assertThat(circuit.allow()).isFalse();

        assertThat(cache.get("catalog", "c", Item.class)).isEmpty();
        cache.put("catalog", "c", new Item("x", BigDecimal.ONE), Duration.ofSeconds(1));
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
    }
}

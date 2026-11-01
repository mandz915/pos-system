package com.pos.common.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.pos.common.event.EventJson;
import com.pos.common.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link JsonCache} on Redis. Every namespace has a version number; entries live under
 * {@code pos:cache:<namespace>:<version>:<key>}. Evicting a namespace increments its version, so thousands of
 * entries are invalidated by one command and the old ones simply expire. Each instance remembers the version for
 * one second, so another instance's eviction is noticed within a second.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisJsonCache implements JsonCache {

    private static final long VERSION_REFRESH_MILLIS = 1_000;

    private record Version(String value, long fetchedAtMillis) {
    }

    private final StringRedisTemplate redis;
    private final RedisCircuit circuit;
    private final ConcurrentHashMap<String, Version> versions = new ConcurrentHashMap<>();

    /**
     * Every tenant has its own copy of each namespace, so one client can never be served another client's cached
     * data, and a catalog change of one client does not flush everybody else's cache. Without a tenant there is no
     * safe place to cache, so nothing is cached.
     */
    private static String scoped(String namespace) {
        Long tenant = TenantContext.currentOrNull();
        return tenant == null ? null : namespace + ":t" + tenant;
    }

    @Override
    public <T> Optional<T> get(String plainNamespace, String key, Class<T> type) {
        String namespace = scoped(plainNamespace);
        if (namespace == null || !circuit.allow()) {
            return Optional.empty();
        }
        String json;
        try {
            json = redis.opsForValue().get(entryKey(namespace, version(namespace), key));
            circuit.success();
        } catch (RuntimeException e) {
            circuit.failure();
            log.warn("Cache read failed, using the database instead: {}", e.getMessage());
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(EventJson.read(json, type));
        } catch (RuntimeException e) {
            log.warn("Ignoring unreadable cache entry {}:{}: {}", namespace, key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String plainNamespace, String key, Object value, Duration ttl) {
        String namespace = scoped(plainNamespace);
        if (namespace == null || !circuit.allow()) {
            return;
        }
        try {
            redis.opsForValue().set(entryKey(namespace, version(namespace), key), EventJson.write(value), ttl);
            circuit.success();
        } catch (RuntimeException e) {
            circuit.failure();
            log.warn("Cache write failed: {}", e.getMessage());
        }
    }

    @Override
    public void evictAll(String plainNamespace) {
        String namespace = scoped(plainNamespace);
        if (namespace == null) {
            return;
        }
        try {
            Long next = redis.opsForValue().increment(versionKey(namespace));
            versions.put(namespace, new Version(String.valueOf(next), System.currentTimeMillis()));
            circuit.success();
        } catch (RuntimeException e) {
            circuit.failure();
            // Entries stay until their TTL runs out, so staleness is bounded by the TTL.
            log.error("Could not evict cache namespace '{}': {}", namespace, e.getMessage());
        }
    }

    private String version(String namespace) {
        long now = System.currentTimeMillis();
        Version known = versions.get(namespace);
        if (known != null && now - known.fetchedAtMillis() < VERSION_REFRESH_MILLIS) {
            return known.value();
        }
        String fetched = redis.opsForValue().get(versionKey(namespace));
        String value = fetched == null ? "0" : fetched;
        versions.put(namespace, new Version(value, now));
        return value;
    }

    static String versionKey(String namespace) {
        return "pos:cache:ver:" + namespace;
    }

    static String entryKey(String namespace, String version, String key) {
        return "pos:cache:" + namespace + ":" + version + ":" + key;
    }
}

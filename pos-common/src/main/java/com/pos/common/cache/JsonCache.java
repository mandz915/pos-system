package com.pos.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Small read-through cache of JSON documents, grouped in namespaces. Failures never propagate: a broken or missing
 * cache behaves like an empty one, so correctness never depends on Redis.
 */
public interface JsonCache {

    <T> Optional<T> get(String namespace, String key, Class<T> type);

    void put(String namespace, String key, Object value, Duration ttl);

    /** Drops every entry of the namespace (O(1): the namespace version is bumped, old entries expire by TTL). */
    void evictAll(String namespace);

    JsonCache NONE = new JsonCache() {
        @Override
        public <T> Optional<T> get(String namespace, String key, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void put(String namespace, String key, Object value, Duration ttl) {
        }

        @Override
        public void evictAll(String namespace) {
        }
    };
}

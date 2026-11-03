package com.pos.common.cache;

import java.time.Duration;
import java.util.Optional;

public interface JsonCache {

    <T> Optional<T> get(String namespace, String key, Class<T> type);

    void put(String namespace, String key, Object value, Duration ttl);

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

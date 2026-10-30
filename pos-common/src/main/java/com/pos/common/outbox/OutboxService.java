package com.pos.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import com.pos.common.event.EventJson;

/**
 * Transactional outbox: events are inserted into the service's own {@code outbox} table in the SAME
 * database transaction as the business change, and relayed to Kafka afterwards by {@link OutboxRelay}.
 * This guarantees "state changed" and "event published" cannot diverge.
 */
public class OutboxService {

    private final JdbcTemplate jdbc;

    public OutboxService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(String topic, String key, Object event) {
        jdbc.update("INSERT INTO outbox (topic, msg_key, payload) VALUES (?, ?, ?)",
                topic, key, EventJson.write(event));
    }
}

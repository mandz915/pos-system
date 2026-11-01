package com.pos.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import com.pos.common.event.EventJson;
import com.pos.common.tenant.TenantContext;

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

    /** The event is stamped with the tenant of the caller; consumers receive it in a Kafka header. */
    public void enqueue(String topic, String key, Object event) {
        jdbc.update("INSERT INTO outbox (topic, msg_key, payload, tenant_id) VALUES (?, ?, ?, ?)",
                topic, key, EventJson.write(event), TenantContext.require());
    }
}

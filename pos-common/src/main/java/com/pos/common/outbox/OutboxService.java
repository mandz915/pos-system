package com.pos.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import com.pos.common.event.EventJson;
import com.pos.common.tenant.TenantContext;

public class OutboxService {

    private final JdbcTemplate jdbc;

    public OutboxService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(String topic, String key, Object event) {
        jdbc.update("INSERT INTO outbox (topic, msg_key, payload, tenant_id) VALUES (?, ?, ?, ?)",
                topic, key, EventJson.write(event), TenantContext.require());
    }
}

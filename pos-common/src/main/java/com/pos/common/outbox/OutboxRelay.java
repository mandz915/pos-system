package com.pos.common.outbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Periodically publishes unpublished outbox rows to Kafka (safe to run on several instances). */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelayString = "${pos.outbox.interval-ms:1000}")
    public void relay() {
        try {
            tx.executeWithoutResult(status -> publishBatch());
        } catch (RuntimeException e) {
            log.warn("Outbox relay failed, will retry: {}", e.getMessage());
        }
    }

    private void publishBatch() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, topic, msg_key, payload FROM outbox WHERE published_at IS NULL "
                        + "ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED");
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            try {
                kafka.send((String) row.get("topic"), (String) row.get("msg_key"), (String) row.get("payload"))
                        .get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Could not publish outbox event {}: {}", id, e.getMessage());
                return; // keep ordering: stop at the first failure and retry on the next tick
            }
            jdbc.update("UPDATE outbox SET published_at = now() WHERE id = ?", id);
        }
    }
}

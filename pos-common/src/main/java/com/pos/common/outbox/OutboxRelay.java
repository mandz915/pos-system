package com.pos.common.outbox;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.pos.common.tenant.TenantHeaders;

import lombok.extern.slf4j.Slf4j;

/**
 * Periodically publishes unpublished outbox rows to Kafka (safe to run on several instances).
 *
 * <p>Throughput: a whole batch is sent asynchronously and acknowledged together, then marked published with one
 * batched update, and the relay keeps draining while batches come back full. Sending row by row (the first version)
 * topped out around 75 events per second, below what 10,000 sales per minute produces.
 *
 * <p>Delivery is at-least-once: if the process dies after Kafka accepted a row but before the update, the row is sent
 * again. Consumers de-duplicate on the event id.
 */
@Slf4j
public class OutboxRelay {

    private static final long MAX_DRAIN_MILLIS = 5_000;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private final int batchSize;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager txManager) {
        this(jdbc, kafka, txManager, 500);
    }

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager txManager,
                       int batchSize) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = new TransactionTemplate(txManager);
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${pos.outbox.interval-ms:200}")
    public void relay() {
        long deadline = System.currentTimeMillis() + MAX_DRAIN_MILLIS;
        try {
            int published;
            do {
                Integer count = tx.execute(status -> publishBatch());
                published = count == null ? 0 : count;
            } while (published >= batchSize && System.currentTimeMillis() < deadline);
        } catch (RuntimeException e) {
            log.warn("Outbox relay failed, will retry: {}", e.getMessage());
        }
    }

    /** Events carry the tenant in a header so consumers can scope their database work to it. */
    private CompletableFuture<?> send(Map<String, Object> row) {
        String topic = (String) row.get("topic");
        String key = (String) row.get("msg_key");
        String payload = (String) row.get("payload");
        Object tenant = row.get("tenant_id");
        if (tenant == null) {
            return kafka.send(topic, key, payload);
        }
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, payload,
                List.of(new RecordHeader(TenantHeaders.KAFKA, String.valueOf(tenant).getBytes(StandardCharsets.UTF_8))));
        return kafka.send(record);
    }

    /** @return the number of rows that were selected (a full batch means there may be more) */
    private int publishBatch() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, topic, msg_key, payload, tenant_id FROM outbox WHERE published_at IS NULL "
                        + "ORDER BY id LIMIT " + batchSize + " FOR UPDATE SKIP LOCKED");
        if (rows.isEmpty()) {
            return 0;
        }
        List<CompletableFuture<?>> sends = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            sends.add(send(row));
        }
        // Mark the longest prefix that Kafka acknowledged; stop at the first failure so order is kept on retry.
        long deadline = System.currentTimeMillis() + 10_000;
        List<Object[]> done = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            try {
                sends.get(i).get(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                done.add(new Object[] {((Number) rows.get(i).get("id")).longValue()});
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Could not publish outbox event {}: {}", rows.get(i).get("id"), e.getMessage());
                break;
            }
        }
        if (!done.isEmpty()) {
            jdbc.batchUpdate("UPDATE outbox SET published_at = now() WHERE id = ?", done);
        }
        return done.size() == rows.size() ? rows.size() : 0; // after a failure do not spin: wait for the next tick
    }
}

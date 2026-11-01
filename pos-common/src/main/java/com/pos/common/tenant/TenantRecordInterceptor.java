package com.pos.common.tenant;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.listener.RecordInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * Puts the tenant of every consumed event into {@link TenantContext} BEFORE the listener (and its transaction)
 * starts, and removes it afterwards. The tenant comes from the record header written by the outbox relay.
 */
@Slf4j
public class TenantRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer) {
        TenantContext.clear();
        Header header = record.headers().lastHeader(TenantHeaders.KAFKA);
        if (header == null || header.value() == null) {
            log.warn("Event on {}-{}@{} has no tenant header; it will not see any data",
                    record.topic(), record.partition(), record.offset());
            return record;
        }
        try {
            TenantContext.set(Long.valueOf(new String(header.value(), StandardCharsets.UTF_8)));
        } catch (NumberFormatException e) {
            log.warn("Event on {}-{}@{} has an unreadable tenant header", record.topic(), record.partition(),
                    record.offset());
        }
        return record;
    }

    @Override
    public void afterRecord(@NonNull ConsumerRecord<Object, Object> record, @NonNull Consumer<Object, Object> consumer) {
        TenantContext.clear();
    }
}

package com.pos.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRecordInterceptorTest {

    private final TenantRecordInterceptor interceptor = new TenantRecordInterceptor();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private static ConsumerRecord<Object, Object> record(String tenantHeader) {
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("pos.sale.completed", 0, 5L, "k", "{}");
        if (tenantHeader != null) {
            record.headers().add(TenantHeaders.KAFKA, tenantHeader.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Test
    void theTenantOfTheEventIsActiveWhileTheListenerRunsAndGoneAfterwards() {
        ConsumerRecord<Object, Object> record = record("12");

        interceptor.intercept(record, null);
        assertThat(TenantContext.currentOrNull()).isEqualTo(12L);

        interceptor.afterRecord(record, null);
        assertThat(TenantContext.currentOrNull()).isNull();
    }

    @Test
    void anEventWithoutATenantGetsNoTenantRatherThanTheLastOnesLeftover() {
        TenantContext.set(99L);

        interceptor.intercept(record(null), null);

        assertThat(TenantContext.currentOrNull()).isNull();
    }

    @Test
    void anUnreadableHeaderGetsNoTenant() {
        interceptor.intercept(record("abc"), null);

        assertThat(TenantContext.currentOrNull()).isNull();
    }
}

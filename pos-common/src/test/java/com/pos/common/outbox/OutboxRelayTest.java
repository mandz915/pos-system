package com.pos.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.pos.common.tenant.TenantHeaders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxRelayTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private KafkaTemplate<String, String> kafka;
    @Mock
    private PlatformTransactionManager txManager;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());
        relay = new OutboxRelay(jdbc, kafka, txManager);
    }

    private static Map<String, Object> row(long id) {
        return Map.of("id", id, "topic", "pos.sale.completed", "msg_key", "k" + id, "payload", "{}");
    }

    private static CompletableFuture<SendResult<String, String>> ok() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> failed() {
        return CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> markedPublished() {
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbc).batchUpdate(eq("UPDATE outbox SET published_at = now() WHERE id = ?"), captor.capture());
        return captor.getValue();
    }

    @Test
    void aWholeBatchIsSentAndMarkedPublishedWithOneBatchedUpdate() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row(1), row(2), row(3)));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(ok());

        relay.relay();

        verify(kafka, org.mockito.Mockito.times(3)).send(anyString(), anyString(), anyString());
        assertThat(markedPublished()).extracting(args -> args[0]).containsExactly(1L, 2L, 3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventsOfATenantCarryItsIdInAHeaderForTheConsumers() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(
                Map.of("id", 1L, "topic", "pos.sale.completed", "msg_key", "k1", "payload", "{}", "tenant_id", 5L)));
        when(kafka.send(org.mockito.ArgumentMatchers.any(ProducerRecord.class))).thenReturn(ok());

        relay.relay();

        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo("pos.sale.completed");
        assertThat(new String(sent.getValue().headers().lastHeader(TenantHeaders.KAFKA).value(),
                StandardCharsets.UTF_8)).isEqualTo("5");
        assertThat(markedPublished()).extracting(args -> args[0]).containsExactly(1L);
    }

    @Test
    void afterAFailureOnlyTheEventsBeforeItAreMarkedSoOrderIsKeptOnRetry() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row(1), row(2), row(3)));
        when(kafka.send(anyString(), eq("k1"), anyString())).thenReturn(ok());
        when(kafka.send(anyString(), eq("k2"), anyString())).thenReturn(failed());
        when(kafka.send(anyString(), eq("k3"), anyString())).thenReturn(ok());

        relay.relay();

        assertThat(markedPublished()).extracting(args -> args[0]).containsExactly(1L);
    }

    @Test
    void nothingIsMarkedWhenTheFirstSendFails() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row(1)));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(failed());

        relay.relay();

        verify(jdbc, never()).batchUpdate(anyString(), org.mockito.ArgumentMatchers.<List<Object[]>>any());
    }

    @Test
    void anEmptyOutboxDoesNothing() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        relay.relay();

        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void aDatabaseErrorIsLoggedAndRetriedOnTheNextTickInsteadOfKillingTheScheduler() {
        when(jdbc.queryForList(anyString())).thenThrow(new IllegalStateException("db down"));

        relay.relay();
    }
}

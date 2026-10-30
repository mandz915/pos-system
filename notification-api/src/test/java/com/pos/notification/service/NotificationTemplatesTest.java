package com.pos.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pos.common.event.GoodsReceivedEvent;
import com.pos.common.event.LowStockEvent;
import com.pos.notification.service.NotificationTemplates.Message;

class NotificationTemplatesTest {

    @Test
    void lowStockMessageNamesProductStoreAndLevels() {
        Message message = NotificationTemplates.lowStock(new LowStockEvent(
                "e1", 3L, 42L, new BigDecimal("4.000"), new BigDecimal("5.000"), Instant.now()));

        assertThat(message.subject()).contains("42").contains("store 3");
        assertThat(message.body()).contains("down to 4").contains("reorder level 5");
    }

    @Test
    void goodsReceivedListsEveryLine() {
        Message message = NotificationTemplates.goodsReceived(new GoodsReceivedEvent(
                "e2", 9L, "PO-000009", 1L, 3L, Instant.now(),
                List.of(new GoodsReceivedEvent.Line(1L, new BigDecimal("10.000"), new BigDecimal("5.00")),
                        new GoodsReceivedEvent.Line(2L, new BigDecimal("2.500"), new BigDecimal("8.00")))));

        assertThat(message.subject()).isEqualTo("Goods received: PO-000009");
        assertThat(message.body()).contains("product 1: 10").contains("product 2: 2.5");
    }
}

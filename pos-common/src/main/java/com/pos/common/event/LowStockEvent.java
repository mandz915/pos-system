package com.pos.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record LowStockEvent(
        String eventId,
        Long storeId,
        Long productId,
        BigDecimal quantity,
        BigDecimal reorderLevel,
        Instant occurredAt) {
}

package com.pos.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Published by inventory-api when a stock level drops to or below its reorder level. */
public record LowStockEvent(
        String eventId,
        Long storeId,
        Long productId,
        BigDecimal quantity,
        BigDecimal reorderLevel,
        Instant occurredAt) {
}

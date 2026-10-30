package com.pos.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Published by sales-api when items of a completed sale are returned. */
public record SaleRefundedEvent(
        String eventId,
        Long saleId,
        Long returnId,
        String receiptNo,
        Long storeId,
        Long customerId,
        BigDecimal refundTotal,
        boolean restock,
        Instant refundedAt,
        List<Line> lines) {

    public record Line(Long productId, String sku, String name, BigDecimal quantity, BigDecimal amount) {
    }
}

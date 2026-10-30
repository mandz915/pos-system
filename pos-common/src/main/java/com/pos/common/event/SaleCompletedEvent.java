package com.pos.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Published by sales-api when a sale is paid and completed. */
public record SaleCompletedEvent(
        String eventId,
        Long saleId,
        String receiptNo,
        Long storeId,
        Long terminalId,
        Long cashierId,
        Long customerId,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal total,
        Instant completedAt,
        List<Line> lines) {

    public record Line(
            Long productId,
            String sku,
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxAmount,
            BigDecimal lineTotal) {
    }
}

package com.pos.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Published by purchasing-api when goods of a purchase order are received; inventory-api adds the stock. */
public record GoodsReceivedEvent(
        String eventId,
        Long purchaseOrderId,
        String poNumber,
        Long receiptId,
        Long storeId,
        Instant receivedAt,
        List<Line> lines) {

    public record Line(Long productId, BigDecimal quantity, BigDecimal unitCost) {
    }
}

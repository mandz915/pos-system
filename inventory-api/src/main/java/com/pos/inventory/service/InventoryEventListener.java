package com.pos.inventory.service;

import java.math.BigDecimal;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.event.EventJson;
import com.pos.common.event.GoodsReceivedEvent;
import com.pos.common.event.SaleCompletedEvent;
import com.pos.common.event.SaleRefundedEvent;
import com.pos.common.event.Topics;
import com.pos.inventory.dto.MovementType;
import com.pos.inventory.mapper.ProcessedEventMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private static final String GROUP = "inventory-api";

    private final StockService stock;
    private final ProcessedEventMapper processed;

    @KafkaListener(topics = Topics.SALE_COMPLETED, groupId = GROUP)
    @Transactional
    public void onSaleCompleted(String payload) {
        SaleCompletedEvent event = EventJson.read(payload, SaleCompletedEvent.class);
        if (processed.markProcessed(event.eventId()) == 0) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }
        for (SaleCompletedEvent.Line line : event.lines()) {
            stock.apply(event.storeId(), line.productId(), line.quantity().negate(), MovementType.SALE,
                    "SALE", String.valueOf(event.saleId()), "Sale " + event.receiptNo(), event.cashierId(), true);
        }
    }

    @KafkaListener(topics = Topics.SALE_REFUNDED, groupId = GROUP)
    @Transactional
    public void onSaleRefunded(String payload) {
        SaleRefundedEvent event = EventJson.read(payload, SaleRefundedEvent.class);
        if (processed.markProcessed(event.eventId()) == 0 || !event.restock()) {
            return;
        }
        for (SaleRefundedEvent.Line line : event.lines()) {
            stock.apply(event.storeId(), line.productId(), line.quantity(), MovementType.RETURN,
                    "RETURN", String.valueOf(event.returnId()), "Return of sale " + event.receiptNo(), null, true);
        }
    }

    @KafkaListener(topics = Topics.GOODS_RECEIVED, groupId = GROUP)
    @Transactional
    public void onGoodsReceived(String payload) {
        GoodsReceivedEvent event = EventJson.read(payload, GoodsReceivedEvent.class);
        if (processed.markProcessed(event.eventId()) == 0) {
            return;
        }
        for (GoodsReceivedEvent.Line line : event.lines()) {
            BigDecimal quantity = line.quantity();
            stock.apply(event.storeId(), line.productId(), quantity, MovementType.RECEIPT,
                    "PURCHASE_ORDER", event.poNumber(), "Goods received for " + event.poNumber(), null, true);
        }
    }
}

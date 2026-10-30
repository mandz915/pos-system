package com.pos.customer.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.event.EventJson;
import com.pos.common.event.SaleCompletedEvent;
import com.pos.common.event.SaleRefundedEvent;
import com.pos.common.event.Topics;
import com.pos.customer.mapper.LedgerMapper;

import lombok.RequiredArgsConstructor;

/** Awards loyalty points for completed sales and takes them back on refunds (idempotent per event). */
@Component
@RequiredArgsConstructor
public class CustomerEventListener {

    private static final String GROUP = "customer-api";

    private final CustomerService customers;
    private final LedgerMapper ledger;

    @KafkaListener(topics = Topics.SALE_COMPLETED, groupId = GROUP)
    @Transactional
    public void onSaleCompleted(String payload) {
        SaleCompletedEvent event = EventJson.read(payload, SaleCompletedEvent.class);
        if (ledger.markProcessed(event.eventId()) == 0 || event.customerId() == null) {
            return;
        }
        customers.earnForSale(event.customerId(), event.total(), event.saleId(), event.receiptNo());
    }

    @KafkaListener(topics = Topics.SALE_REFUNDED, groupId = GROUP)
    @Transactional
    public void onSaleRefunded(String payload) {
        SaleRefundedEvent event = EventJson.read(payload, SaleRefundedEvent.class);
        if (ledger.markProcessed(event.eventId()) == 0 || event.customerId() == null) {
            return;
        }
        customers.reverseForRefund(event.customerId(), event.refundTotal(), event.saleId(), event.receiptNo());
    }
}

package com.pos.reporting.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.event.EventJson;
import com.pos.common.event.SaleCompletedEvent;
import com.pos.common.event.SaleRefundedEvent;
import com.pos.common.event.Topics;
import com.pos.reporting.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private static final String GROUP = "reporting-api";

    private final ReportMapper reports;

    @Value("${reporting.timezone:Asia/Manila}")
    private String timezone;

    @KafkaListener(topics = Topics.SALE_COMPLETED, groupId = GROUP)
    @Transactional
    public void onSaleCompleted(String payload) {
        SaleCompletedEvent event = EventJson.read(payload, SaleCompletedEvent.class);
        if (reports.markProcessed(event.eventId()) == 0) {
            return;
        }
        LocalDate day = localDay(event.completedAt());
        reports.addSale(event.storeId(), day, event.total(), event.discountTotal(), event.taxTotal());
        reports.addCashier(event.storeId(), day, event.cashierId(), event.total());
        for (SaleCompletedEvent.Line line : event.lines()) {
            reports.addProduct(event.storeId(), day, line.productId(), line.sku(), line.name(),
                    line.quantity(), line.lineTotal());
        }
    }

    @KafkaListener(topics = Topics.SALE_REFUNDED, groupId = GROUP)
    @Transactional
    public void onSaleRefunded(String payload) {
        SaleRefundedEvent event = EventJson.read(payload, SaleRefundedEvent.class);
        if (reports.markProcessed(event.eventId()) == 0) {
            return;
        }
        LocalDate day = localDay(event.refundedAt());
        reports.addRefund(event.storeId(), day, event.refundTotal());
        for (SaleRefundedEvent.Line line : event.lines()) {
            reports.addProduct(event.storeId(), day, line.productId(), line.sku(), line.name(),
                    line.quantity().negate(), line.amount().negate());
        }
    }

    private LocalDate localDay(Instant instant) {
        return instant.atZone(ZoneId.of(timezone)).toLocalDate();
    }
}

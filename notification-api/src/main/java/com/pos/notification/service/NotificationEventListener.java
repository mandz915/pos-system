package com.pos.notification.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.pos.common.event.EventJson;
import com.pos.common.event.GoodsReceivedEvent;
import com.pos.common.event.LowStockEvent;
import com.pos.common.event.Topics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String GROUP = "notification-api";

    private final NotificationService notifications;

    @KafkaListener(topics = Topics.LOW_STOCK, groupId = GROUP)
    public void onLowStock(String payload) {
        LowStockEvent event = EventJson.read(payload, LowStockEvent.class);
        notifications.queue(event.eventId(), "LOW_STOCK", event.storeId(), NotificationTemplates.lowStock(event));
    }

    @KafkaListener(topics = Topics.GOODS_RECEIVED, groupId = GROUP)
    public void onGoodsReceived(String payload) {
        GoodsReceivedEvent event = EventJson.read(payload, GoodsReceivedEvent.class);
        notifications.queue(event.eventId(), "GOODS_RECEIVED", event.storeId(), NotificationTemplates.goodsReceived(event));
    }
}

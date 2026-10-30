package com.pos.notification.service;

import com.pos.common.event.GoodsReceivedEvent;
import com.pos.common.event.LowStockEvent;

/** Message texts, kept pure so they can be unit tested. */
public final class NotificationTemplates {

    private NotificationTemplates() {
    }

    public record Message(String subject, String body) {
    }

    public static Message lowStock(LowStockEvent event) {
        String quantity = event.quantity().stripTrailingZeros().toPlainString();
        String reorder = event.reorderLevel().stripTrailingZeros().toPlainString();
        return new Message(
                "Low stock: product " + event.productId() + " (store " + event.storeId() + ")",
                "Product " + event.productId() + " in store " + event.storeId() + " is down to " + quantity
                        + " (reorder level " + reorder + "). Please reorder.");
    }

    public static Message goodsReceived(GoodsReceivedEvent event) {
        StringBuilder body = new StringBuilder("Goods were received for ")
                .append(event.poNumber()).append(" in store ").append(event.storeId()).append(":\n");
        for (GoodsReceivedEvent.Line line : event.lines()) {
            body.append("- product ").append(line.productId()).append(": ")
                    .append(line.quantity().stripTrailingZeros().toPlainString()).append('\n');
        }
        return new Message("Goods received: " + event.poNumber(), body.toString());
    }
}

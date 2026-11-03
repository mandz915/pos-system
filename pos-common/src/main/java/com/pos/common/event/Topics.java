package com.pos.common.event;

public final class Topics {

    public static final String SALE_COMPLETED = "pos.sale.completed";
    public static final String SALE_REFUNDED = "pos.sale.refunded";
    public static final String GOODS_RECEIVED = "pos.goods.received";
    public static final String LOW_STOCK = "pos.inventory.low-stock";

    private Topics() {
    }
}

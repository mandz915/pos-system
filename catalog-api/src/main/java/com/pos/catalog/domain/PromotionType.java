package com.pos.catalog.domain;

public enum PromotionType {
    /** value = percentage off the unit price. */
    PERCENT,
    /** value = fixed amount off the unit price. */
    FIXED,
    /** buyQty units paid, getQty units of the same product free, repeating (needs buyQty and getQty). */
    BUY_X_GET_Y
}

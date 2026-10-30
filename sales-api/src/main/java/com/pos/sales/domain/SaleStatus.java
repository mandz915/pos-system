package com.pos.sales.domain;

public enum SaleStatus {
    /** Being rung up at the till. */
    OPEN,
    /** Parked so the cashier can serve someone else. */
    HELD,
    COMPLETED,
    /** Cancelled before payment. */
    VOIDED,
    /** Every item has been returned. */
    REFUNDED
}

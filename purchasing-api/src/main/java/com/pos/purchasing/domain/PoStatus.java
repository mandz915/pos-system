package com.pos.purchasing.domain;

public enum PoStatus {
    /** Being prepared; lines can be edited. */
    DRAFT,
    /** Sent to the supplier; goods can be received. */
    ORDERED,
    /** Some, but not all, goods received. */
    PARTIAL,
    RECEIVED,
    CANCELLED
}

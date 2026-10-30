package com.pos.inventory.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class StockCount {
    private Long id;
    private Long storeId;
    /** OPEN or POSTED. */
    private String status;
    private Long createdBy;
    private Instant createdAt;
    private Instant postedAt;
}

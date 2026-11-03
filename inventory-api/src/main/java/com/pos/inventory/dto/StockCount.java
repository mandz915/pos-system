package com.pos.inventory.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class StockCount {
    private Long id;
    private Long storeId;

    private String status;
    private Long createdBy;
    private Instant createdAt;
    private Instant postedAt;
}

package com.pos.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class StockMovement {
    private Long id;
    private Long storeId;
    private Long productId;
    private MovementType type;

    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private String refType;
    private String refId;
    private String note;
    private Long createdBy;
    private Instant createdAt;
}

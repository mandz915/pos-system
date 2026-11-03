package com.pos.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class StockLevel {
    private Long id;
    private Long storeId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal reorderLevel;
    private Instant updatedAt;
}

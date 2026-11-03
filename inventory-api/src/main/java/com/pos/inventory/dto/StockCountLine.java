package com.pos.inventory.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class StockCountLine {
    private Long id;
    private Long countId;
    private Long productId;

    private BigDecimal systemQty;
    private BigDecimal countedQty;
}

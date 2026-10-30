package com.pos.inventory.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class StockCountLine {
    private Long id;
    private Long countId;
    private Long productId;
    /** Filled when the count is posted. */
    private BigDecimal systemQty;
    private BigDecimal countedQty;
}

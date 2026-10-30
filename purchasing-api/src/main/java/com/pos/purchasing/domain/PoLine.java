package com.pos.purchasing.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PoLine {
    private Long id;
    private Long poId;
    private Long productId;
    private BigDecimal orderedQty;
    private BigDecimal receivedQty = BigDecimal.ZERO;
    private BigDecimal unitCost;

    public BigDecimal outstanding() {
        return orderedQty.subtract(receivedQty);
    }
}

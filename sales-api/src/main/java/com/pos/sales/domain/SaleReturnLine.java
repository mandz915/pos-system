package com.pos.sales.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SaleReturnLine {
    private Long id;
    private Long returnId;
    private Long saleLineId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal amount;
}

package com.pos.sales.domain;

import java.math.BigDecimal;

import lombok.Data;

/** A line is a price snapshot: later catalog changes never alter a recorded sale. */
@Data
public class SaleLine {
    private Long id;
    private Long saleId;
    private Integer lineNo;
    private Long productId;
    private String sku;
    private String name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPerUnit = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private boolean taxInclusive = true;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal lineTotal;
    private BigDecimal returnedQty = BigDecimal.ZERO;
}

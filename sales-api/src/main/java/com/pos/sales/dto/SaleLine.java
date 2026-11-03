package com.pos.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

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
    private String taxCode;

    private boolean taxOverridden;
    private boolean taxInclusive = true;
    private BigDecimal taxAmount = BigDecimal.ZERO;

    private String promotionCode;
    private BigDecimal lineTotal;
    private BigDecimal returnedQty = BigDecimal.ZERO;
}

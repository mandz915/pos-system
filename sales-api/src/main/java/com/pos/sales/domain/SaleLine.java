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
    private String taxCode;
    /** True when a supervisor replaced the catalog tax on this line. */
    private boolean taxOverridden;
    private boolean taxInclusive = true;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    /** Code of the promotion that produced {@code discountTotal}, if any. */
    private String promotionCode;
    private BigDecimal lineTotal;
    private BigDecimal returnedQty = BigDecimal.ZERO;
}

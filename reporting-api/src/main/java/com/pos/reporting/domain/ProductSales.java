package com.pos.reporting.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductSales {
    private Long productId;
    private String sku;
    private String name;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal revenue = BigDecimal.ZERO;
}

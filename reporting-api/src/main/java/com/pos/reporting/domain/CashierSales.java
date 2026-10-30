package com.pos.reporting.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CashierSales {
    private Long cashierId;
    private long salesCount;
    private BigDecimal gross = BigDecimal.ZERO;
}

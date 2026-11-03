package com.pos.reporting.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CashierSales {
    private Long cashierId;
    private long salesCount;
    private BigDecimal gross = BigDecimal.ZERO;
}

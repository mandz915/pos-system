package com.pos.sales.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ShiftSalesTotals {
    private long salesCount;
    private BigDecimal gross = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
}

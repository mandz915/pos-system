package com.pos.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RefundTotals {
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal cash = BigDecimal.ZERO;
}

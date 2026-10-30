package com.pos.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class DailySales {
    private Long storeId;
    private LocalDate day;
    private long salesCount;
    private BigDecimal gross = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private BigDecimal taxTotal = BigDecimal.ZERO;
    private long refundCount;
    private BigDecimal refundTotal = BigDecimal.ZERO;
}

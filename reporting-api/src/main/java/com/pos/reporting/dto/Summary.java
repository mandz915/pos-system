package com.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Summary(Long storeId, LocalDate from, LocalDate to, long salesCount, BigDecimal gross,
                      BigDecimal discountTotal, BigDecimal taxTotal, long refundCount, BigDecimal refundTotal,
                      BigDecimal netSales, BigDecimal averageSale) {
}

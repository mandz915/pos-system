package com.pos.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class Sale {
    private Long id;

    private String clientRef;
    private String receiptNo;
    private Long storeId;
    private Long terminalId;
    private Long shiftId;
    private Long cashierId;
    private Long customerId;
    private SaleStatus status;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private BigDecimal taxTotal = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal paidTotal = BigDecimal.ZERO;
    private BigDecimal changeDue = BigDecimal.ZERO;
    private String voidReason;
    private Instant createdAt;
    private Instant completedAt;
}

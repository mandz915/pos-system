package com.pos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class Sale {
    private Long id;
    /** Client generated UUID: makes creation idempotent so offline tills can safely retry. */
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

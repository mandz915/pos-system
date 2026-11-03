package com.pos.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.model.PaymentMethod;

import lombok.Data;

@Data
public class Payment {
    private Long id;
    private PaymentKind kind;
    private Long saleId;
    private Long storeId;
    private PaymentMethod method;

    private BigDecimal amount;

    private BigDecimal tendered;
    private PaymentStatus status;
    private String reference;
    private String gatewayRef;
    private String reason;
    private Long createdBy;
    private Instant createdAt;
}

package com.pos.payment.domain;

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
    /** Amount applied to the sale (what the sale was credited with). */
    private BigDecimal amount;
    /** Cash handed over by the customer; may exceed {@code amount} when change is given. */
    private BigDecimal tendered;
    private PaymentStatus status;
    private String reference;
    private String gatewayRef;
    private String reason;
    private Long createdBy;
    private Instant createdAt;
}

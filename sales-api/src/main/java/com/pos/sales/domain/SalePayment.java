package com.pos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.model.PaymentMethod;

import lombok.Data;

@Data
public class SalePayment {
    private Long id;
    private Long saleId;
    private PaymentMethod method;
    /** Amount credited to the sale. */
    private BigDecimal amount;
    /** What the customer handed over (cash can exceed {@code amount}; the difference is the change). */
    private BigDecimal tendered;
    private String reference;
    private Long paymentId;
    private String gatewayRef;
    private Instant createdAt;
}

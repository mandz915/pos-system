package com.pos.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.model.PaymentMethod;

import lombok.Data;

@Data
public class SalePayment {
    private Long id;
    private Long saleId;
    private PaymentMethod method;

    private BigDecimal amount;

    private BigDecimal tendered;
    private String reference;
    private Long paymentId;
    private String gatewayRef;
    private Instant createdAt;
}

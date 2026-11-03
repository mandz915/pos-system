package com.pos.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.model.PaymentMethod;

import lombok.Data;

@Data
public class SaleReturn {
    private Long id;
    private Long saleId;
    private Long shiftId;
    private BigDecimal refundTotal;
    private PaymentMethod method;
    private String reason;
    private boolean restock = true;
    private Long createdBy;
    private Instant createdAt;
}

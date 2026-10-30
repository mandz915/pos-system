package com.pos.payment.web;

import java.math.BigDecimal;

import com.pos.common.model.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record ChargeRequest(
            @NotNull Long saleId,
            @NotNull Long storeId,
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            /** Cash handed over (>= amount when change is given). Defaults to amount. */
            BigDecimal tendered,
            @Size(max = 100) String reference) {
    }

    public record RefundCall(
            @NotNull Long saleId,
            @NotNull Long storeId,
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Size(max = 255) String reason) {
    }
}

package com.pos.payment.gateway;

import java.math.BigDecimal;

import com.pos.common.model.PaymentMethod;

public interface PaymentGateway {

    GatewayResult authorize(PaymentMethod method, BigDecimal amount, String reference);

    GatewayResult refund(PaymentMethod method, BigDecimal amount, String reference);

    record GatewayResult(boolean approved, String gatewayRef, String message) {
    }
}

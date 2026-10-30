package com.pos.payment.gateway;

import java.math.BigDecimal;

import com.pos.common.model.PaymentMethod;

/**
 * Abstraction over an external payment provider (card acquirer, e-wallet). Add an implementation and select it with
 * {@code payment.gateway.provider} to integrate a real provider; cash never goes through the gateway.
 */
public interface PaymentGateway {

    GatewayResult authorize(PaymentMethod method, BigDecimal amount, String reference);

    GatewayResult refund(PaymentMethod method, BigDecimal amount, String reference);

    record GatewayResult(boolean approved, String gatewayRef, String message) {
    }
}

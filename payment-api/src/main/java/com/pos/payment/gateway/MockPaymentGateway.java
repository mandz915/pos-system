package com.pos.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pos.common.model.PaymentMethod;

/** Development gateway: approves everything, except a payment whose reference is "DECLINE". */
@Component
@ConditionalOnProperty(name = "payment.gateway.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult authorize(PaymentMethod method, BigDecimal amount, String reference) {
        if ("DECLINE".equalsIgnoreCase(reference)) {
            return new GatewayResult(false, null, "Declined by mock gateway");
        }
        return new GatewayResult(true, "MOCK-" + UUID.randomUUID(), "Approved");
    }

    @Override
    public GatewayResult refund(PaymentMethod method, BigDecimal amount, String reference) {
        return new GatewayResult(true, "MOCK-REFUND-" + UUID.randomUUID(), "Refunded");
    }
}

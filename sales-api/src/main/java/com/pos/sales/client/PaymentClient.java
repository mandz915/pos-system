package com.pos.sales.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.pos.common.model.PaymentMethod;
import com.pos.sales.service.TenderPlanner.Tender;

public interface PaymentClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentResult(Long id, String status, String gatewayRef, String reason) {
    }

    record ChargeBody(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, BigDecimal tendered,
                             String reference) {
    }

    record RefundBody(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, String reason) {
    }

    PaymentResult charge(Long saleId, Long storeId, Tender tender);

    PaymentResult refund(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, String reason);

    PaymentResult reverse(Long paymentId);
}

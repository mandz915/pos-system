package com.pos.payment.service;

import java.time.LocalDate;
import java.util.List;

import com.pos.payment.dto.Payment;
import com.pos.payment.dto.ReconciliationRow;
import com.pos.payment.dto.PaymentDtos.ChargeRequest;
import com.pos.payment.dto.PaymentDtos.RefundCall;

public interface PaymentService {

    Payment charge(ChargeRequest request);

    Payment refund(RefundCall request);

    Payment reverse(Long paymentId);

    Payment get(Long id);

    List<Payment> forSale(Long saleId);

    List<ReconciliationRow> reconciliation(Long storeId, LocalDate date);
}

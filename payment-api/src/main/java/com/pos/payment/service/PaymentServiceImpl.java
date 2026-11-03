package com.pos.payment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.util.Money;
import com.pos.payment.dto.Payment;
import com.pos.payment.dto.PaymentKind;
import com.pos.payment.dto.PaymentStatus;
import com.pos.payment.dto.ReconciliationRow;
import com.pos.payment.gateway.PaymentGateway;
import com.pos.payment.gateway.PaymentGateway.GatewayResult;
import com.pos.payment.mapper.PaymentMapper;
import com.pos.payment.dto.PaymentDtos.ChargeRequest;
import com.pos.payment.dto.PaymentDtos.RefundCall;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper payments;
    private final PaymentGateway gateway;

    @Value("${payment.timezone:Asia/Manila}")
    private String timezone;

    @Override
    public Payment charge(ChargeRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        GatewayResult result = request.method() == PaymentMethod.CASH
                ? new GatewayResult(true, null, "Cash")
                : gateway.authorize(request.method(), request.amount(), request.reference());

        Payment payment = new Payment();
        payment.setKind(PaymentKind.PAYMENT);
        payment.setSaleId(request.saleId());
        payment.setStoreId(request.storeId());
        payment.setMethod(request.method());
        payment.setAmount(Money.round(request.amount()));
        payment.setTendered(request.tendered() == null ? Money.round(request.amount()) : Money.round(request.tendered()));
        payment.setStatus(result.approved() ? PaymentStatus.APPROVED : PaymentStatus.DECLINED);
        payment.setReference(request.reference());
        payment.setGatewayRef(result.gatewayRef());
        payment.setReason(result.approved() ? null : result.message());
        payment.setCreatedBy(CurrentUser.get().userId());
        payments.insert(payment);
        return payments.findById(payment.getId());
    }

    @Override
    public Payment refund(RefundCall request) {
        CurrentUser.requireStoreAccess(request.storeId());
        BigDecimal paid = payments.sumApproved(request.saleId(), PaymentKind.PAYMENT.name());
        BigDecimal refunded = payments.sumApproved(request.saleId(), PaymentKind.REFUND.name());
        BigDecimal refundable = paid.subtract(refunded);
        if (request.amount().compareTo(refundable) > 0) {
            throw ApiException.conflict("Refund of " + request.amount() + " exceeds the refundable amount "
                    + refundable + " for sale " + request.saleId());
        }
        GatewayResult result = request.method() == PaymentMethod.CASH
                ? new GatewayResult(true, null, "Cash")
                : gateway.refund(request.method(), request.amount(), String.valueOf(request.saleId()));
        if (!result.approved()) {
            throw ApiException.conflict("Refund was rejected by the payment provider: " + result.message());
        }

        Payment payment = new Payment();
        payment.setKind(PaymentKind.REFUND);
        payment.setSaleId(request.saleId());
        payment.setStoreId(request.storeId());
        payment.setMethod(request.method());
        payment.setAmount(Money.round(request.amount()));
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setGatewayRef(result.gatewayRef());
        payment.setReason(request.reason());
        payment.setCreatedBy(CurrentUser.get().userId());
        payments.insert(payment);
        return payments.findById(payment.getId());
    }

    @Override
    public Payment reverse(Long paymentId) {
        Payment original = payments.findById(paymentId);
        if (original == null || original.getKind() != PaymentKind.PAYMENT) {
            throw ApiException.notFound("Payment", paymentId);
        }
        CurrentUser.requireStoreAccess(original.getStoreId());
        boolean owner = CurrentUser.get().userId().equals(original.getCreatedBy());
        if (!owner && !CurrentUser.get().hasAnyRole(Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR)) {
            throw ApiException.forbidden("Only the user who took the payment or a supervisor can reverse it");
        }
        if (original.getStatus() != PaymentStatus.APPROVED) {
            throw ApiException.conflict("Only an approved payment can be reversed");
        }
        if (original.getCreatedAt().isBefore(java.time.Instant.now().minus(java.time.Duration.ofMinutes(15)))) {
            throw ApiException.conflict("The reversal window has passed; use a refund instead");
        }
        return refund(new RefundCall(original.getSaleId(), original.getStoreId(), original.getMethod(),
                original.getAmount(), "Reversal of payment " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment get(Long id) {
        Payment payment = payments.findById(id);
        if (payment == null) {
            throw ApiException.notFound("Payment", id);
        }
        CurrentUser.requireStoreAccess(payment.getStoreId());
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> forSale(Long saleId) {
        List<Payment> list = payments.findBySale(saleId);
        list.forEach(p -> CurrentUser.requireStoreAccess(p.getStoreId()));
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationRow> reconciliation(Long storeId, LocalDate date) {
        CurrentUser.requireStoreAccess(storeId);
        ZoneId zone = ZoneId.of(timezone);
        return payments.reconciliation(storeId,
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant());
    }
}

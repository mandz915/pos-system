package com.pos.payment.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.payment.domain.Payment;
import com.pos.payment.domain.ReconciliationRow;
import com.pos.payment.service.PaymentService;
import com.pos.payment.web.PaymentDtos.ChargeRequest;
import com.pos.payment.web.PaymentDtos.RefundCall;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    /** Called by sales-api at checkout (with the cashier's token). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment charge(@Valid @RequestBody ChargeRequest request) {
        return service.charge(request);
    }

    @PostMapping("/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public Payment refund(@Valid @RequestBody RefundCall request) {
        return service.refund(request);
    }

    /** Undo a payment taken moments ago (used when a checkout fails half-way). */
    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment reverse(@PathVariable Long id) {
        return service.reverse(id);
    }

    @GetMapping("/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public Payment get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public List<Payment> forSale(@RequestParam Long saleId) {
        return service.forSale(saleId);
    }

    @GetMapping("/reconciliation")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public List<ReconciliationRow> reconciliation(@RequestParam Long storeId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.reconciliation(storeId, date);
    }
}

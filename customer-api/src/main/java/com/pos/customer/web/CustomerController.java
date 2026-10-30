package com.pos.customer.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.web.ActiveRequest;
import com.pos.common.web.PageResult;
import com.pos.customer.domain.CreditTransaction;
import com.pos.customer.domain.Customer;
import com.pos.customer.domain.LoyaltyTransaction;
import com.pos.customer.service.CustomerService;
import com.pos.customer.web.CustomerDtos.AdjustPointsRequest;
import com.pos.customer.web.CustomerDtos.CreditRequest;
import com.pos.customer.web.CustomerDtos.CustomerRequest;
import com.pos.customer.web.CustomerDtos.PointsRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public PageResult<Customer> search(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        return service.search(q, page, size);
    }

    @GetMapping("/{id}")
    public Customer get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@Valid @RequestBody CustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Customer setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return service.setActive(id, request.active());
    }

    // ---- loyalty
    @GetMapping("/{id}/loyalty")
    public PageResult<LoyaltyTransaction> loyalty(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return service.loyaltyHistory(id, page, size);
    }

    @PostMapping("/{id}/loyalty/redeem")
    public Customer redeem(@PathVariable Long id, @Valid @RequestBody PointsRequest request) {
        return service.redeemPoints(id, request.points(), request.note());
    }

    @PostMapping("/{id}/loyalty/adjust")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Customer adjust(@PathVariable Long id, @Valid @RequestBody AdjustPointsRequest request) {
        return service.adjustPoints(id, request.points(), request.note());
    }

    // ---- store credit
    @GetMapping("/{id}/credit")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public PageResult<CreditTransaction> credit(@PathVariable Long id,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        return service.creditHistory(id, page, size);
    }

    @PostMapping("/{id}/credit/charge")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public Customer charge(@PathVariable Long id, @Valid @RequestBody CreditRequest request) {
        return service.charge(id, request);
    }

    @PostMapping("/{id}/credit/payment")
    public Customer payCredit(@PathVariable Long id, @Valid @RequestBody CreditRequest request) {
        return service.payCredit(id, request);
    }
}

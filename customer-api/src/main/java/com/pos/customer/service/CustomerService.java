package com.pos.customer.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.util.Money;
import com.pos.common.web.PageResult;
import com.pos.common.web.Paging;
import com.pos.customer.domain.CreditTransaction;
import com.pos.customer.domain.Customer;
import com.pos.customer.domain.LoyaltyTransaction;
import com.pos.customer.mapper.CustomerMapper;
import com.pos.customer.mapper.LedgerMapper;
import com.pos.customer.web.CustomerDtos.CreditRequest;
import com.pos.customer.web.CustomerDtos.CustomerRequest;

import lombok.RequiredArgsConstructor;

/** Customer profiles plus two ledgers: loyalty points and store credit (a running tab). */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerMapper customers;
    private final LedgerMapper ledger;

    @Value("${loyalty.points-per-currency:0.01}")
    private BigDecimal pointsPerCurrency;

    // ------------------------------------------------------------------ profiles
    @Transactional(readOnly = true)
    public PageResult<Customer> search(String q, Integer page, Integer size) {
        Paging paging = Paging.of(page, size);
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
        return PageResult.of(customers.search(pattern, paging.size(), paging.offset()), paging, customers.count(pattern));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        Customer customer = customers.findById(id);
        if (customer == null) {
            throw ApiException.notFound("Customer", id);
        }
        return customer;
    }

    public Customer create(CustomerRequest request) {
        Customer customer = new Customer();
        apply(customer, request);
        customer.setCode(request.code() == null || request.code().isBlank()
                ? "C" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : request.code().trim().toUpperCase());
        customers.insert(customer);
        return get(customer.getId());
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        if (request.creditLimit() != null && Money.round(request.creditLimit()).compareTo(customer.getCreditBalance()) < 0) {
            throw ApiException.conflict("The credit limit cannot be lower than the current balance "
                    + customer.getCreditBalance());
        }
        apply(customer, request);
        customers.update(customer);
        return get(id);
    }

    public Customer setActive(Long id, boolean active) {
        get(id);
        customers.setActive(id, active);
        return get(id);
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setCreditLimit(request.creditLimit() == null ? BigDecimal.ZERO : Money.round(request.creditLimit()));
    }

    // ------------------------------------------------------------------ loyalty
    /** Awards points for a completed sale; the caller guarantees idempotency (processed event). */
    public void earnForSale(Long customerId, BigDecimal saleTotal, Long saleId, String receiptNo) {
        long points = LoyaltyCalculator.pointsFor(saleTotal, pointsPerCurrency);
        if (points <= 0 || customers.findById(customerId) == null) {
            return;
        }
        movePoints(customerId, points, "EARN", "SALE", String.valueOf(saleId), "Sale " + receiptNo);
    }

    /** Takes back points that were earned on goods that were returned. */
    public void reverseForRefund(Long customerId, BigDecimal refundTotal, Long saleId, String receiptNo) {
        Customer customer = customers.lock(customerId);
        if (customer == null) {
            return;
        }
        long points = LoyaltyCalculator.pointsToReverse(refundTotal, pointsPerCurrency, customer.getPointsBalance());
        if (points > 0) {
            movePoints(customerId, -points, "REVERSE", "SALE", String.valueOf(saleId), "Refund of sale " + receiptNo);
        }
    }

    public Customer redeemPoints(Long customerId, long points, String note) {
        movePoints(customerId, -points, "REDEEM", "MANUAL", null, note);
        return get(customerId);
    }

    public Customer adjustPoints(Long customerId, long points, String note) {
        if (points == 0) {
            throw ApiException.badRequest("points must not be zero");
        }
        movePoints(customerId, points, "ADJUST", "MANUAL", null, note);
        return get(customerId);
    }

    private void movePoints(Long customerId, long delta, String type, String refType, String refId, String note) {
        Customer customer = customers.lock(customerId);
        if (customer == null) {
            throw ApiException.notFound("Customer", customerId);
        }
        long after = customer.getPointsBalance() + delta;
        if (after < 0) {
            throw ApiException.conflict("Insufficient points: balance is " + customer.getPointsBalance());
        }
        customers.setPoints(customerId, after);
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setCustomerId(customerId);
        transaction.setType(type);
        transaction.setPoints(delta);
        transaction.setBalance(after);
        transaction.setRefType(refType);
        transaction.setRefId(refId);
        transaction.setNote(note);
        ledger.insertLoyalty(transaction);
    }

    @Transactional(readOnly = true)
    public PageResult<LoyaltyTransaction> loyaltyHistory(Long customerId, Integer page, Integer size) {
        get(customerId);
        Paging paging = Paging.of(page, size);
        return PageResult.of(ledger.loyaltyHistory(customerId, paging.size(), paging.offset()), paging,
                ledger.loyaltyCount(customerId));
    }

    // ------------------------------------------------------------------ store credit
    /** Puts an amount on the customer's tab, within the credit limit. */
    public Customer charge(Long customerId, CreditRequest request) {
        Customer customer = customers.lock(customerId);
        if (customer == null) {
            throw ApiException.notFound("Customer", customerId);
        }
        if (!customer.isActive()) {
            throw ApiException.conflict("Customer " + customerId + " is inactive");
        }
        BigDecimal amount = Money.round(request.amount());
        BigDecimal after = customer.getCreditBalance().add(amount);
        if (after.compareTo(customer.getCreditLimit()) > 0) {
            throw ApiException.conflict("Credit limit exceeded: limit " + customer.getCreditLimit()
                    + ", would owe " + after);
        }
        record(customer, "CHARGE", amount, after, request);
        return get(customerId);
    }

    /** The customer pays down their tab. */
    public Customer payCredit(Long customerId, CreditRequest request) {
        Customer customer = customers.lock(customerId);
        if (customer == null) {
            throw ApiException.notFound("Customer", customerId);
        }
        BigDecimal amount = Money.round(request.amount());
        if (amount.compareTo(customer.getCreditBalance()) > 0) {
            throw ApiException.conflict("Payment of " + amount + " exceeds the amount owed "
                    + customer.getCreditBalance());
        }
        record(customer, "PAYMENT", amount, customer.getCreditBalance().subtract(amount), request);
        return get(customerId);
    }

    private void record(Customer customer, String type, BigDecimal amount, BigDecimal balanceAfter, CreditRequest request) {
        customers.setCreditBalance(customer.getId(), balanceAfter);
        CreditTransaction transaction = new CreditTransaction();
        transaction.setCustomerId(customer.getId());
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalance(balanceAfter);
        transaction.setRefType(request.refType());
        transaction.setRefId(request.refId());
        transaction.setNote(request.note());
        ledger.insertCredit(transaction);
    }

    @Transactional(readOnly = true)
    public PageResult<CreditTransaction> creditHistory(Long customerId, Integer page, Integer size) {
        get(customerId);
        Paging paging = Paging.of(page, size);
        return PageResult.of(ledger.creditHistory(customerId, paging.size(), paging.offset()), paging,
                ledger.creditCount(customerId));
    }
}

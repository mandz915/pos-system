package com.pos.customer.service;

import java.math.BigDecimal;

import com.pos.common.dto.PageResult;
import com.pos.customer.dto.CreditTransaction;
import com.pos.customer.dto.Customer;
import com.pos.customer.dto.LoyaltyTransaction;
import com.pos.customer.dto.CustomerDtos.CreditRequest;
import com.pos.customer.dto.CustomerDtos.CustomerRequest;

public interface CustomerService {

    PageResult<Customer> search(String q, Integer page, Integer size);

    Customer get(Long id);

    Customer create(CustomerRequest request);

    Customer update(Long id, CustomerRequest request);

    Customer setActive(Long id, boolean active);

    void earnForSale(Long customerId, BigDecimal saleTotal, Long saleId, String receiptNo);

    void reverseForRefund(Long customerId, BigDecimal refundTotal, Long saleId, String receiptNo);

    Customer redeemPoints(Long customerId, long points, String note);

    Customer adjustPoints(Long customerId, long points, String note);

    PageResult<LoyaltyTransaction> loyaltyHistory(Long customerId, Integer page, Integer size);

    Customer charge(Long customerId, CreditRequest request);

    Customer payCredit(Long customerId, CreditRequest request);

    PageResult<CreditTransaction> creditHistory(Long customerId, Integer page, Integer size);
}

package com.pos.payment.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.payment.domain.Payment;
import com.pos.payment.domain.ReconciliationRow;

@Mapper
public interface PaymentMapper {

    int insert(Payment payment);

    Payment findById(@Param("id") Long id);

    List<Payment> findBySale(@Param("saleId") Long saleId);

    /** Sum of approved rows of one kind ('PAYMENT' or 'REFUND') for a sale. */
    BigDecimal sumApproved(@Param("saleId") Long saleId, @Param("kind") String kind);

    List<ReconciliationRow> reconciliation(@Param("storeId") Long storeId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);
}

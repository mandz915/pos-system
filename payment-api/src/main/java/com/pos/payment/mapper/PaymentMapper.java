package com.pos.payment.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.payment.domain.Payment;
import com.pos.payment.domain.ReconciliationRow;

@Mapper
public interface PaymentMapper {

    @Insert("""
            INSERT INTO payments (kind, sale_id, store_id, method, amount, tendered, status, reference, gateway_ref, reason, created_by)
            VALUES (#{kind}, #{saleId}, #{storeId}, #{method}, #{amount}, #{tendered}, #{status}, #{reference}, #{gatewayRef}, #{reason}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Payment payment);

    @Select("SELECT * FROM payments WHERE id = #{id}")
    Payment findById(@Param("id") Long id);

    @Select("SELECT * FROM payments WHERE sale_id = #{saleId} ORDER BY id")
    List<Payment> findBySale(@Param("saleId") Long saleId);

    /** Sum of approved rows of one kind ('PAYMENT' or 'REFUND') for a sale. */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE sale_id = #{saleId} AND kind = #{kind} AND status = 'APPROVED'")
    BigDecimal sumApproved(@Param("saleId") Long saleId, @Param("kind") String kind);

    @Select("""
            SELECT method, kind, status, COUNT(*) AS count, COALESCE(SUM(amount), 0) AS total
            FROM payments
            WHERE store_id = #{storeId} AND created_at >= #{from} AND created_at < #{to}
            GROUP BY method, kind, status
            ORDER BY method, kind, status
            """)
    List<ReconciliationRow> reconciliation(@Param("storeId") Long storeId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);
}

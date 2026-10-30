package com.pos.sales.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.sales.domain.SalePayment;

@Mapper
public interface SalePaymentMapper {

    @Insert("""
            INSERT INTO sale_payments (sale_id, method, amount, tendered, reference, payment_id, gateway_ref)
            VALUES (#{saleId}, #{method}, #{amount}, #{tendered}, #{reference}, #{paymentId}, #{gatewayRef})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SalePayment payment);

    @Select("SELECT * FROM sale_payments WHERE sale_id = #{saleId} ORDER BY id")
    List<SalePayment> listBySale(@Param("saleId") Long saleId);
}

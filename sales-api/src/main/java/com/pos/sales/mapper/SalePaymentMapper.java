package com.pos.sales.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.domain.SalePayment;

@Mapper
public interface SalePaymentMapper {

    int insert(SalePayment payment);

    List<SalePayment> listBySale(@Param("saleId") Long saleId);
}

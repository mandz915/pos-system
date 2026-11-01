package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.domain.SaleLine;

@Mapper
public interface SaleLineMapper {

    int insert(SaleLine line);

    List<SaleLine> listBySale(@Param("saleId") Long saleId);

    void deleteBySale(@Param("saleId") Long saleId);

    void addReturned(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    long countNotFullyReturned(@Param("saleId") Long saleId);
}

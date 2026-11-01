package com.pos.sales.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.domain.SaleReturn;
import com.pos.sales.domain.SaleReturnLine;

@Mapper
public interface ReturnMapper {

    int insert(SaleReturn saleReturn);

    void insertLine(SaleReturnLine line);

    List<SaleReturn> listBySale(@Param("saleId") Long saleId);
}

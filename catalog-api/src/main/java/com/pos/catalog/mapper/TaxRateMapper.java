package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.catalog.dto.TaxRate;

@Mapper
public interface TaxRateMapper {

    int insert(TaxRate taxRate);

    TaxRate findById(@Param("id") Long id);

    List<TaxRate> findAll();

    int update(TaxRate taxRate);
}

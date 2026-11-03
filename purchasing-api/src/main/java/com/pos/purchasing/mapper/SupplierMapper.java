package com.pos.purchasing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.purchasing.dto.Supplier;

@Mapper
public interface SupplierMapper {

    int insert(Supplier supplier);

    Supplier findById(@Param("id") Long id);

    List<Supplier> findAll(@Param("q") String q);

    int update(Supplier supplier);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}

package com.pos.purchasing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.purchasing.domain.Supplier;

@Mapper
public interface SupplierMapper {

    void insert(Supplier supplier);

    Supplier findById(@Param("id") Long id);

    List<Supplier> findAll(@Param("q") String q);

    void update(Supplier supplier);

    void setActive(@Param("id") Long id, @Param("active") boolean active);
}

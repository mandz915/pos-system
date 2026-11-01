package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.domain.Store;

@Mapper
public interface StoreMapper {

    void insert(Store store);

    Store findById(@Param("id") Long id);

    List<Store> findAll();

    void update(Store store);

    void setActive(@Param("id") Long id, @Param("active") boolean active);
}

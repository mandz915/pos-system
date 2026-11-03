package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.dto.Store;

@Mapper
public interface StoreMapper {

    int insert(Store store);

    Store findById(@Param("id") Long id);

    List<Store> findAll();

    int update(Store store);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}

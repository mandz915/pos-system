package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.catalog.dto.Promotion;

@Mapper
public interface PromotionMapper {

    int insert(Promotion promotion);

    Promotion findById(@Param("id") Long id);

    List<Promotion> findAll();

    List<Promotion> findLive();

    int update(Promotion promotion);
}

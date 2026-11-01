package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.catalog.domain.Promotion;

@Mapper
public interface PromotionMapper {

    int insert(Promotion promotion);

    Promotion findById(@Param("id") Long id);

    List<Promotion> findAll();

    /** Promotions that are switched on and inside their validity window right now. */
    List<Promotion> findLive();

    int update(Promotion promotion);
}

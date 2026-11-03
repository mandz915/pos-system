package com.pos.inventory.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.inventory.dto.StockMovement;

@Mapper
public interface MovementMapper {

    int insert(StockMovement movement);

    List<StockMovement> search(@Param("storeId") Long storeId,
                               @Param("productId") Long productId,
                               @Param("size") int size,
                               @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("productId") Long productId);
}

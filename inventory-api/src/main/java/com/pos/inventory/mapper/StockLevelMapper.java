package com.pos.inventory.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.inventory.domain.StockLevel;

@Mapper
public interface StockLevelMapper {

    void ensure(@Param("storeId") Long storeId, @Param("productId") Long productId);

    StockLevel lock(@Param("storeId") Long storeId, @Param("productId") Long productId);

    StockLevel find(@Param("storeId") Long storeId, @Param("productId") Long productId);

    void updateQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    void updateReorderLevel(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("level") BigDecimal level);

    List<StockLevel> search(@Param("storeId") Long storeId,
                            @Param("productId") Long productId,
                            @Param("lowOnly") boolean lowOnly,
                            @Param("size") int size,
                            @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("lowOnly") boolean lowOnly);
}

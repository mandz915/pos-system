package com.pos.inventory.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.inventory.domain.StockCount;
import com.pos.inventory.domain.StockCountLine;

@Mapper
public interface StockCountMapper {

    int insert(StockCount count);

    StockCount findById(@Param("id") Long id);

    StockCount lock(@Param("id") Long id);

    List<StockCount> findAll(@Param("storeId") Long storeId);

    void markPosted(@Param("id") Long id);

    void upsertLine(@Param("countId") Long countId, @Param("productId") Long productId, @Param("countedQty") BigDecimal countedQty);

    void setSystemQty(@Param("id") Long id, @Param("systemQty") BigDecimal systemQty);

    List<StockCountLine> lines(@Param("countId") Long countId);
}

package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.domain.Sale;
import com.pos.sales.domain.SaleStatus;

@Mapper
public interface SaleMapper {

    int insert(Sale sale);

    Sale findById(@Param("id") Long id);

    Sale lock(@Param("id") Long id);

    Sale findByClientRef(@Param("clientRef") String clientRef);

    void updateTotals(Sale sale);

    void updateStatus(@Param("id") Long id, @Param("status") SaleStatus status);

    void voidSale(@Param("id") Long id, @Param("reason") String reason);

    void complete(@Param("id") Long id,
                  @Param("receiptNo") String receiptNo,
                  @Param("paidTotal") BigDecimal paidTotal,
                  @Param("changeDue") BigDecimal changeDue,
                  @Param("completedAt") Instant completedAt);

    /** Atomically takes the next gapless receipt number of the store. */
    long nextReceiptNo(@Param("storeId") Long storeId);

    List<Sale> search(@Param("storeId") Long storeId,
                      @Param("status") String status,
                      @Param("from") Instant from,
                      @Param("to") Instant to,
                      @Param("size") int size,
                      @Param("offset") int offset);

    long count(@Param("storeId") Long storeId,
               @Param("status") String status,
               @Param("from") Instant from,
               @Param("to") Instant to);
}

package com.pos.reporting.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.reporting.domain.CashierSales;
import com.pos.reporting.domain.DailySales;
import com.pos.reporting.domain.ProductSales;

@Mapper
public interface ReportMapper {

    // ---------------------------------------------------------------- writes (event handlers)
    int addSale(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("gross") BigDecimal gross,
                @Param("discount") BigDecimal discount, @Param("tax") BigDecimal tax);

    int addRefund(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("amount") BigDecimal amount);

    /** Adds (or, with negative values, takes away) quantity and revenue of one product on one day. */
    int addProduct(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("productId") Long productId,
                   @Param("sku") String sku, @Param("name") String name,
                   @Param("quantity") BigDecimal quantity, @Param("revenue") BigDecimal revenue);

    int addCashier(@Param("storeId") Long storeId, @Param("day") LocalDate day,
                   @Param("cashierId") Long cashierId, @Param("gross") BigDecimal gross);

    /** @return 1 when the event is new, 0 when it was already processed. */
    int markProcessed(@Param("eventId") String eventId);

    // ---------------------------------------------------------------- reads
    List<DailySales> daily(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<ProductSales> topProducts(@Param("storeId") Long storeId, @Param("from") LocalDate from,
                                   @Param("to") LocalDate to, @Param("limit") int limit);

    List<CashierSales> byCashier(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}

package com.pos.reporting.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.reporting.dto.CashierSales;
import com.pos.reporting.dto.DailySales;
import com.pos.reporting.dto.ProductSales;

@Mapper
public interface ReportMapper {

    int addSale(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("gross") BigDecimal gross,
                @Param("discount") BigDecimal discount, @Param("tax") BigDecimal tax);

    int addRefund(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("amount") BigDecimal amount);

    int addProduct(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("productId") Long productId,
                   @Param("sku") String sku, @Param("name") String name,
                   @Param("quantity") BigDecimal quantity, @Param("revenue") BigDecimal revenue);

    int addCashier(@Param("storeId") Long storeId, @Param("day") LocalDate day,
                   @Param("cashierId") Long cashierId, @Param("gross") BigDecimal gross);

    int markProcessed(@Param("eventId") String eventId);

    List<DailySales> daily(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<ProductSales> topProducts(@Param("storeId") Long storeId, @Param("from") LocalDate from,
                                   @Param("to") LocalDate to, @Param("limit") int limit);

    List<CashierSales> byCashier(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}

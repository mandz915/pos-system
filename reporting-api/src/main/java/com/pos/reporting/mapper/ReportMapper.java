package com.pos.reporting.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.reporting.domain.CashierSales;
import com.pos.reporting.domain.DailySales;
import com.pos.reporting.domain.ProductSales;

@Mapper
public interface ReportMapper {

    // ---------------------------------------------------------------- writes (event handlers)
    @Insert("""
            INSERT INTO sales_daily (store_id, day, sales_count, gross, discount_total, tax_total)
            VALUES (#{storeId}, #{day}, 1, #{gross}, #{discount}, #{tax})
            ON CONFLICT (store_id, day) DO UPDATE SET
                sales_count = sales_daily.sales_count + 1,
                gross = sales_daily.gross + EXCLUDED.gross,
                discount_total = sales_daily.discount_total + EXCLUDED.discount_total,
                tax_total = sales_daily.tax_total + EXCLUDED.tax_total
            """)
    int addSale(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("gross") BigDecimal gross,
                @Param("discount") BigDecimal discount, @Param("tax") BigDecimal tax);

    @Insert("""
            INSERT INTO sales_daily (store_id, day, refund_count, refund_total)
            VALUES (#{storeId}, #{day}, 1, #{amount})
            ON CONFLICT (store_id, day) DO UPDATE SET
                refund_count = sales_daily.refund_count + 1,
                refund_total = sales_daily.refund_total + EXCLUDED.refund_total
            """)
    int addRefund(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("amount") BigDecimal amount);

    /** Adds (or, with negative values, takes away) quantity and revenue of one product on one day. */
    @Insert("""
            INSERT INTO product_daily (store_id, day, product_id, sku, name, quantity, revenue)
            VALUES (#{storeId}, #{day}, #{productId}, #{sku}, #{name}, #{quantity}, #{revenue})
            ON CONFLICT (store_id, day, product_id) DO UPDATE SET
                quantity = product_daily.quantity + EXCLUDED.quantity,
                revenue = product_daily.revenue + EXCLUDED.revenue,
                sku = COALESCE(EXCLUDED.sku, product_daily.sku),
                name = COALESCE(EXCLUDED.name, product_daily.name)
            """)
    int addProduct(@Param("storeId") Long storeId, @Param("day") LocalDate day, @Param("productId") Long productId,
                   @Param("sku") String sku, @Param("name") String name,
                   @Param("quantity") BigDecimal quantity, @Param("revenue") BigDecimal revenue);

    @Insert("""
            INSERT INTO cashier_daily (store_id, day, cashier_id, sales_count, gross)
            VALUES (#{storeId}, #{day}, #{cashierId}, 1, #{gross})
            ON CONFLICT (store_id, day, cashier_id) DO UPDATE SET
                sales_count = cashier_daily.sales_count + 1,
                gross = cashier_daily.gross + EXCLUDED.gross
            """)
    int addCashier(@Param("storeId") Long storeId, @Param("day") LocalDate day,
                   @Param("cashierId") Long cashierId, @Param("gross") BigDecimal gross);

    /** @return 1 when the event is new, 0 when it was already processed. */
    @Insert("INSERT INTO processed_events (event_id) VALUES (#{eventId}) ON CONFLICT DO NOTHING")
    int markProcessed(@Param("eventId") String eventId);

    // ---------------------------------------------------------------- reads
    @Select("""
            <script>
            SELECT store_id, day, sales_count, gross, discount_total, tax_total, refund_count, refund_total
            FROM sales_daily
            WHERE day BETWEEN #{from} AND #{to}
            <if test="storeId != null">AND store_id = #{storeId}</if>
            ORDER BY day, store_id
            </script>
            """)
    List<DailySales> daily(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            <script>
            SELECT product_id, MAX(sku) AS sku, MAX(name) AS name, SUM(quantity) AS quantity, SUM(revenue) AS revenue
            FROM product_daily
            WHERE day BETWEEN #{from} AND #{to}
            <if test="storeId != null">AND store_id = #{storeId}</if>
            GROUP BY product_id
            HAVING SUM(quantity) &gt; 0
            ORDER BY SUM(revenue) DESC, product_id
            LIMIT #{limit}
            </script>
            """)
    List<ProductSales> topProducts(@Param("storeId") Long storeId, @Param("from") LocalDate from,
                                   @Param("to") LocalDate to, @Param("limit") int limit);

    @Select("""
            <script>
            SELECT cashier_id, SUM(sales_count) AS sales_count, SUM(gross) AS gross
            FROM cashier_daily
            WHERE day BETWEEN #{from} AND #{to}
            <if test="storeId != null">AND store_id = #{storeId}</if>
            GROUP BY cashier_id
            ORDER BY SUM(gross) DESC
            </script>
            """)
    List<CashierSales> byCashier(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}

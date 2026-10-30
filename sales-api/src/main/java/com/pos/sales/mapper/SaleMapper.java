package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.sales.domain.Sale;
import com.pos.sales.domain.SaleStatus;

@Mapper
public interface SaleMapper {

    @Insert("""
            INSERT INTO sales (client_ref, store_id, terminal_id, shift_id, cashier_id, customer_id, status)
            VALUES (#{clientRef}, #{storeId}, #{terminalId}, #{shiftId}, #{cashierId}, #{customerId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Sale sale);

    @Select("SELECT * FROM sales WHERE id = #{id}")
    Sale findById(@Param("id") Long id);

    @Select("SELECT * FROM sales WHERE id = #{id} FOR UPDATE")
    Sale lock(@Param("id") Long id);

    @Select("SELECT * FROM sales WHERE client_ref = #{clientRef}")
    Sale findByClientRef(@Param("clientRef") String clientRef);

    @Update("""
            UPDATE sales SET subtotal = #{subtotal}, discount_total = #{discountTotal},
                tax_total = #{taxTotal}, total = #{total}
            WHERE id = #{id}
            """)
    int updateTotals(Sale sale);

    @Update("UPDATE sales SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") SaleStatus status);

    @Update("UPDATE sales SET status = 'VOIDED', void_reason = #{reason} WHERE id = #{id}")
    int voidSale(@Param("id") Long id, @Param("reason") String reason);

    @Update("""
            UPDATE sales SET status = 'COMPLETED', receipt_no = #{receiptNo}, paid_total = #{paidTotal},
                change_due = #{changeDue}, completed_at = #{completedAt}
            WHERE id = #{id}
            """)
    int complete(@Param("id") Long id,
                 @Param("receiptNo") String receiptNo,
                 @Param("paidTotal") BigDecimal paidTotal,
                 @Param("changeDue") BigDecimal changeDue,
                 @Param("completedAt") Instant completedAt);

    /** Atomically takes the next gapless receipt number of the store. */
    @Select("""
            INSERT INTO receipt_sequences (store_id, last_no) VALUES (#{storeId}, 1)
            ON CONFLICT (store_id) DO UPDATE SET last_no = receipt_sequences.last_no + 1
            RETURNING last_no
            """)
    long nextReceiptNo(@Param("storeId") Long storeId);

    @Select("""
            <script>
            SELECT * FROM sales
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="from != null">AND created_at &gt;= #{from}</if>
              <if test="to != null">AND created_at &lt; #{to}</if>
            </where>
            ORDER BY id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Sale> search(@Param("storeId") Long storeId,
                      @Param("status") String status,
                      @Param("from") Instant from,
                      @Param("to") Instant to,
                      @Param("size") int size,
                      @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM sales
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="from != null">AND created_at &gt;= #{from}</if>
              <if test="to != null">AND created_at &lt; #{to}</if>
            </where>
            </script>
            """)
    long count(@Param("storeId") Long storeId,
               @Param("status") String status,
               @Param("from") Instant from,
               @Param("to") Instant to);
}

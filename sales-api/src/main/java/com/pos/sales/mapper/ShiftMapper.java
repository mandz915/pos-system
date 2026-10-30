package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.sales.domain.AmountByLabel;
import com.pos.sales.domain.CashMovement;
import com.pos.sales.domain.RefundTotals;
import com.pos.sales.domain.Shift;
import com.pos.sales.domain.ShiftSalesTotals;

@Mapper
public interface ShiftMapper {

    @Insert("""
            INSERT INTO shifts (store_id, terminal_id, cashier_id, status, opening_cash)
            VALUES (#{storeId}, #{terminalId}, #{cashierId}, #{status}, #{openingCash})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Shift shift);

    @Select("SELECT * FROM shifts WHERE id = #{id}")
    Shift findById(@Param("id") Long id);

    @Select("SELECT * FROM shifts WHERE id = #{id} FOR UPDATE")
    Shift lock(@Param("id") Long id);

    @Select("SELECT * FROM shifts WHERE terminal_id = #{terminalId} AND status = 'OPEN'")
    Shift findOpenByTerminal(@Param("terminalId") Long terminalId);

    @Update("""
            UPDATE shifts SET status = 'CLOSED', closing_cash = #{closingCash}, expected_cash = #{expectedCash},
                closed_at = now()
            WHERE id = #{id}
            """)
    int close(@Param("id") Long id,
              @Param("closingCash") BigDecimal closingCash,
              @Param("expectedCash") BigDecimal expectedCash);

    @Insert("""
            INSERT INTO cash_movements (shift_id, type, amount, reason, created_by)
            VALUES (#{shiftId}, #{type}, #{amount}, #{reason}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertCashMovement(CashMovement movement);

    @Select("SELECT * FROM cash_movements WHERE shift_id = #{shiftId} ORDER BY id")
    List<CashMovement> cashMovements(@Param("shiftId") Long shiftId);

    // ---- aggregates used by the X/Z read
    @Select("""
            SELECT COUNT(*) AS sales_count,
                   COALESCE(SUM(total), 0) AS gross,
                   COALESCE(SUM(discount_total), 0) AS discount,
                   COALESCE(SUM(tax_total), 0) AS tax
            FROM sales
            WHERE shift_id = #{shiftId} AND status IN ('COMPLETED', 'REFUNDED')
            """)
    ShiftSalesTotals salesTotals(@Param("shiftId") Long shiftId);

    @Select("""
            SELECT p.method AS label, COALESCE(SUM(p.amount), 0) AS amount
            FROM sale_payments p JOIN sales s ON s.id = p.sale_id
            WHERE s.shift_id = #{shiftId} AND s.status IN ('COMPLETED', 'REFUNDED')
            GROUP BY p.method
            """)
    List<AmountByLabel> paymentsByMethod(@Param("shiftId") Long shiftId);

    @Select("""
            SELECT type AS label, COALESCE(SUM(amount), 0) AS amount
            FROM cash_movements WHERE shift_id = #{shiftId} GROUP BY type
            """)
    List<AmountByLabel> cashMovementTotals(@Param("shiftId") Long shiftId);

    @Select("""
            SELECT COALESCE(SUM(refund_total), 0) AS total,
                   COALESCE(SUM(CASE WHEN method = 'CASH' THEN refund_total ELSE 0 END), 0) AS cash
            FROM sale_returns WHERE shift_id = #{shiftId}
            """)
    RefundTotals refundTotals(@Param("shiftId") Long shiftId);
}

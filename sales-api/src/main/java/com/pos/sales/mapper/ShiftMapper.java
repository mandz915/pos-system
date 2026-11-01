package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.domain.AmountByLabel;
import com.pos.sales.domain.CashMovement;
import com.pos.sales.domain.RefundTotals;
import com.pos.sales.domain.Shift;
import com.pos.sales.domain.ShiftSalesTotals;

@Mapper
public interface ShiftMapper {

    int insert(Shift shift);

    Shift findById(@Param("id") Long id);

    Shift lock(@Param("id") Long id);

    Shift findOpenByTerminal(@Param("terminalId") Long terminalId);

    void close(@Param("id") Long id,
               @Param("closingCash") BigDecimal closingCash,
               @Param("expectedCash") BigDecimal expectedCash);

    void insertCashMovement(CashMovement movement);

    // ---- aggregates used by the X/Z read
    ShiftSalesTotals salesTotals(@Param("shiftId") Long shiftId);

    List<AmountByLabel> paymentsByMethod(@Param("shiftId") Long shiftId);

    List<AmountByLabel> cashMovementTotals(@Param("shiftId") Long shiftId);

    RefundTotals refundTotals(@Param("shiftId") Long shiftId);
}

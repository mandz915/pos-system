package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.sales.dto.AmountByLabel;
import com.pos.sales.dto.CashMovement;
import com.pos.sales.dto.RefundTotals;
import com.pos.sales.dto.Shift;
import com.pos.sales.dto.ShiftSalesTotals;

@Mapper
public interface ShiftMapper {

    int insert(Shift shift);

    Shift findById(@Param("id") Long id);

    Shift lock(@Param("id") Long id);

    Shift findOpenByTerminal(@Param("terminalId") Long terminalId);

    int close(@Param("id") Long id,
              @Param("closingCash") BigDecimal closingCash,
              @Param("expectedCash") BigDecimal expectedCash);

    int insertCashMovement(CashMovement movement);

    List<CashMovement> cashMovements(@Param("shiftId") Long shiftId);

    ShiftSalesTotals salesTotals(@Param("shiftId") Long shiftId);

    List<AmountByLabel> paymentsByMethod(@Param("shiftId") Long shiftId);

    List<AmountByLabel> cashMovementTotals(@Param("shiftId") Long shiftId);

    RefundTotals refundTotals(@Param("shiftId") Long shiftId);
}

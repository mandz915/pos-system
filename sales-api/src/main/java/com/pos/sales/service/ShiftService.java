package com.pos.sales.service;

import com.pos.sales.dto.CashMovement;
import com.pos.sales.dto.Shift;
import com.pos.sales.dto.SalesDtos.CashMovementRequest;
import com.pos.sales.dto.SalesDtos.CloseShiftRequest;
import com.pos.sales.dto.SalesDtos.OpenShiftRequest;
import com.pos.sales.dto.SalesDtos.ShiftReport;

public interface ShiftService {

    Shift open(OpenShiftRequest request);

    Shift get(Long id);

    Shift current(Long terminalId);

    CashMovement addCashMovement(Long shiftId, CashMovementRequest request);

    ShiftReport xRead(Long shiftId);

    ShiftReport close(Long shiftId, CloseShiftRequest request);
}

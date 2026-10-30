package com.pos.sales.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.util.Money;
import com.pos.sales.domain.AmountByLabel;
import com.pos.sales.domain.CashMovement;
import com.pos.sales.domain.RefundTotals;
import com.pos.sales.domain.Shift;
import com.pos.sales.domain.ShiftSalesTotals;
import com.pos.sales.domain.ShiftStatus;
import com.pos.sales.mapper.ShiftMapper;
import com.pos.sales.web.SalesDtos.CashMovementRequest;
import com.pos.sales.web.SalesDtos.CloseShiftRequest;
import com.pos.sales.web.SalesDtos.OpenShiftRequest;
import com.pos.sales.web.SalesDtos.ShiftReport;

import lombok.RequiredArgsConstructor;

/** Till shifts: opening float, cash in/out, X-read (while open) and Z-read (closing with a cash count). */
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftService {

    private final ShiftMapper shifts;

    public Shift open(OpenShiftRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        if (shifts.findOpenByTerminal(request.terminalId()) != null) {
            throw ApiException.conflict("Terminal " + request.terminalId() + " already has an open shift");
        }
        Shift shift = new Shift();
        shift.setStoreId(request.storeId());
        shift.setTerminalId(request.terminalId());
        shift.setCashierId(CurrentUser.get().userId());
        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpeningCash(Money.round(request.openingCash()));
        shifts.insert(shift);
        return shifts.findById(shift.getId());
    }

    @Transactional(readOnly = true)
    public Shift get(Long id) {
        Shift shift = shifts.findById(id);
        if (shift == null) {
            throw ApiException.notFound("Shift", id);
        }
        CurrentUser.requireStoreAccess(shift.getStoreId());
        return shift;
    }

    @Transactional(readOnly = true)
    public Shift current(Long terminalId) {
        Shift shift = shifts.findOpenByTerminal(terminalId);
        if (shift == null) {
            throw ApiException.notFound("Open shift for terminal", terminalId);
        }
        CurrentUser.requireStoreAccess(shift.getStoreId());
        return shift;
    }

    public CashMovement addCashMovement(Long shiftId, CashMovementRequest request) {
        Shift shift = shifts.lock(shiftId);
        if (shift == null) {
            throw ApiException.notFound("Shift", shiftId);
        }
        CurrentUser.requireStoreAccess(shift.getStoreId());
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw ApiException.conflict("Shift " + shiftId + " is closed");
        }
        CashMovement movement = new CashMovement();
        movement.setShiftId(shiftId);
        movement.setType(request.type());
        movement.setAmount(Money.round(request.amount()));
        movement.setReason(request.reason());
        movement.setCreatedBy(CurrentUser.get().userId());
        shifts.insertCashMovement(movement);
        return movement;
    }

    @Transactional(readOnly = true)
    public ShiftReport xRead(Long shiftId) {
        return report(get(shiftId), null);
    }

    /** Z-read: closes the shift, storing the counted cash and the expected cash computed from the system. */
    public ShiftReport close(Long shiftId, CloseShiftRequest request) {
        Shift shift = shifts.lock(shiftId);
        if (shift == null) {
            throw ApiException.notFound("Shift", shiftId);
        }
        CurrentUser.requireStoreAccess(shift.getStoreId());
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw ApiException.conflict("Shift " + shiftId + " is already closed");
        }
        AuthenticatedUser user = CurrentUser.get();
        if (!user.userId().equals(shift.getCashierId())
                && !user.hasAnyRole(Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR)) {
            throw ApiException.forbidden("Only the cashier of the shift or a supervisor can close it");
        }
        BigDecimal counted = Money.round(request.countedCash());
        ShiftReport preview = report(shift, counted);
        shifts.close(shiftId, counted, preview.expectedCash());
        return report(shifts.findById(shiftId), counted);
    }

    private ShiftReport report(Shift shift, BigDecimal countedOverride) {
        Long id = shift.getId();
        ShiftSalesTotals totals = shifts.salesTotals(id);
        RefundTotals refunds = shifts.refundTotals(id);

        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        for (AmountByLabel row : shifts.paymentsByMethod(id)) {
            byMethod.put(row.getLabel(), row.getAmount());
        }
        BigDecimal cashIn = Money.zero();
        BigDecimal cashOut = Money.zero();
        for (AmountByLabel row : shifts.cashMovementTotals(id)) {
            if ("CASH_IN".equals(row.getLabel())) {
                cashIn = row.getAmount();
            } else if ("CASH_OUT".equals(row.getLabel())) {
                cashOut = row.getAmount();
            }
        }
        BigDecimal cashSales = byMethod.getOrDefault("CASH", Money.zero());
        BigDecimal expected = shift.getOpeningCash()
                .add(cashSales)
                .subtract(refunds.getCash())
                .add(cashIn)
                .subtract(cashOut);
        BigDecimal counted = countedOverride != null ? countedOverride : shift.getClosingCash();
        BigDecimal variance = counted == null ? null : counted.subtract(expected);

        return new ShiftReport(id, shift.getStoreId(), shift.getTerminalId(), shift.getCashierId(),
                shift.getStatus().name(), shift.getOpenedAt(), shift.getClosedAt(), shift.getOpeningCash(),
                totals.getSalesCount(), totals.getGross(), totals.getDiscount(), totals.getTax(),
                refunds.getTotal(), totals.getGross().subtract(refunds.getTotal()), byMethod,
                cashIn, cashOut, expected, counted, variance);
    }
}

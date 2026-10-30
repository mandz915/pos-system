package com.pos.sales.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.sales.domain.CashMovement;
import com.pos.sales.domain.Shift;
import com.pos.sales.service.ShiftService;
import com.pos.sales.web.SalesDtos.CashMovementRequest;
import com.pos.sales.web.SalesDtos.CloseShiftRequest;
import com.pos.sales.web.SalesDtos.OpenShiftRequest;
import com.pos.sales.web.SalesDtos.ShiftReport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public Shift open(@Valid @RequestBody OpenShiftRequest request) {
        return shiftService.open(request);
    }

    /** The open shift of a terminal (404 when the till has no open shift). */
    @GetMapping("/current")
    public Shift current(@RequestParam Long terminalId) {
        return shiftService.current(terminalId);
    }

    @GetMapping("/{id}")
    public Shift get(@PathVariable Long id) {
        return shiftService.get(id);
    }

    @GetMapping("/{id}/x-read")
    public ShiftReport xRead(@PathVariable Long id) {
        return shiftService.xRead(id);
    }

    @PostMapping("/{id}/close")
    public ShiftReport close(@PathVariable Long id, @Valid @RequestBody CloseShiftRequest request) {
        return shiftService.close(id, request);
    }

    @PostMapping("/{id}/cash-movements")
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovement cashMovement(@PathVariable Long id, @Valid @RequestBody CashMovementRequest request) {
        return shiftService.addCashMovement(id, request);
    }
}

package com.pos.sales.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.dto.PageResult;
import com.pos.sales.domain.Sale;
import com.pos.sales.service.ReceiptService;
import com.pos.sales.service.SaleService;
import com.pos.sales.dto.SalesDtos.CheckoutRequest;
import com.pos.sales.dto.SalesDtos.CreateSaleRequest;
import com.pos.sales.dto.SalesDtos.RefundRequest;
import com.pos.sales.dto.SalesDtos.ReplaceLinesRequest;
import com.pos.sales.dto.SalesDtos.SaleView;
import com.pos.sales.dto.SalesDtos.VoidRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final ReceiptService receiptService;

    /** Creates an open sale. Send a client generated {@code clientRef} (UUID) so retries are safe. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleView create(@Valid @RequestBody CreateSaleRequest request) {
        return saleService.create(request);
    }

    @GetMapping
    public PageResult<Sale> search(@RequestParam(required = false) Long storeId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        return saleService.search(storeId, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    public SaleView get(@PathVariable Long id) {
        return saleService.view(id);
    }

    @PutMapping("/{id}/lines")
    public SaleView replaceLines(@PathVariable Long id, @Valid @RequestBody ReplaceLinesRequest request) {
        return saleService.replaceLines(id, request.lines(), request.couponCodes());
    }

    @PostMapping("/{id}/hold")
    public SaleView hold(@PathVariable Long id) {
        return saleService.hold(id);
    }

    @PostMapping("/{id}/resume")
    public SaleView resume(@PathVariable Long id) {
        return saleService.resume(id);
    }

    @PostMapping("/{id}/checkout")
    public SaleView checkout(@PathVariable Long id, @Valid @RequestBody CheckoutRequest request) {
        return saleService.checkout(id, request);
    }

    /** Cancels an unpaid sale. */
    @PostMapping("/{id}/void")
    public SaleView voidSale(@PathVariable Long id, @Valid @RequestBody VoidRequest request) {
        return saleService.voidSale(id, request.reason());
    }

    /** Returns goods of a completed sale (supervisor level). */
    @PostMapping("/{id}/refund")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public SaleView refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        return saleService.refund(id, request);
    }

    @GetMapping(value = "/{id}/receipt", produces = "text/plain;charset=UTF-8")
    public String receipt(@PathVariable Long id) {
        return receiptService.render(id);
    }
}

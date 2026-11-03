package com.pos.reporting.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.reporting.dto.CashierSales;
import com.pos.reporting.dto.DailySales;
import com.pos.reporting.dto.ProductSales;
import com.pos.reporting.service.ReportService;
import com.pos.reporting.dto.Summary;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports/sales")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
public class ReportController {

    private final ReportService service;

    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) Long storeId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(storeId, from, to);
    }

    @GetMapping("/daily")
    public List<DailySales> daily(@RequestParam(required = false) Long storeId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.daily(storeId, from, to);
    }

    @GetMapping(value = "/daily.csv", produces = "text/csv")
    public ResponseEntity<String> dailyCsv(@RequestParam(required = false) Long storeId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-daily-" + from + "-" + to + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.dailyCsv(storeId, from, to));
    }

    @GetMapping("/top-products")
    public List<ProductSales> topProducts(@RequestParam(required = false) Long storeId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(required = false) Integer limit) {
        return service.topProducts(storeId, from, to, limit);
    }

    @GetMapping("/by-cashier")
    public List<CashierSales> byCashier(@RequestParam(required = false) Long storeId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.byCashier(storeId, from, to);
    }
}

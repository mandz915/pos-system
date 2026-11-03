package com.pos.reporting.service;

import java.time.LocalDate;
import java.util.List;

import com.pos.reporting.dto.CashierSales;
import com.pos.reporting.dto.DailySales;
import com.pos.reporting.dto.ProductSales;
import com.pos.reporting.dto.Summary;

public interface ReportService {

    List<DailySales> daily(Long storeId, LocalDate from, LocalDate to);

    Summary summary(Long storeId, LocalDate from, LocalDate to);

    List<ProductSales> topProducts(Long storeId, LocalDate from, LocalDate to, Integer limit);

    List<CashierSales> byCashier(Long storeId, LocalDate from, LocalDate to);

    String dailyCsv(Long storeId, LocalDate from, LocalDate to);
}

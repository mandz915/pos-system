package com.pos.reporting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.security.CurrentUser;
import com.pos.common.util.Money;
import com.pos.reporting.domain.CashierSales;
import com.pos.reporting.domain.DailySales;
import com.pos.reporting.domain.ProductSales;
import com.pos.reporting.dto.Summary;
import com.pos.reporting.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int MAX_RANGE_DAYS = 366;

    private final ReportMapper reports;

    public List<DailySales> daily(Long storeId, LocalDate from, LocalDate to) {
        Long store = scope(storeId);
        checkRange(from, to);
        return reports.daily(store, from, to);
    }

    public Summary summary(Long storeId, LocalDate from, LocalDate to) {
        Long store = scope(storeId);
        checkRange(from, to);
        long salesCount = 0;
        long refundCount = 0;
        BigDecimal gross = Money.zero();
        BigDecimal discount = Money.zero();
        BigDecimal tax = Money.zero();
        BigDecimal refunds = Money.zero();
        for (DailySales day : reports.daily(store, from, to)) {
            salesCount += day.getSalesCount();
            refundCount += day.getRefundCount();
            gross = gross.add(day.getGross());
            discount = discount.add(day.getDiscountTotal());
            tax = tax.add(day.getTaxTotal());
            refunds = refunds.add(day.getRefundTotal());
        }
        BigDecimal average = salesCount == 0 ? Money.zero()
                : gross.divide(BigDecimal.valueOf(salesCount), 2, java.math.RoundingMode.HALF_UP);
        return new Summary(store, from, to, salesCount, gross, discount, tax, refundCount, refunds,
                gross.subtract(refunds), average);
    }

    public List<ProductSales> topProducts(Long storeId, LocalDate from, LocalDate to, Integer limit) {
        Long store = scope(storeId);
        checkRange(from, to);
        int size = limit == null ? 10 : Math.clamp(limit, 1, 100);
        return reports.topProducts(store, from, to, size);
    }

    public List<CashierSales> byCashier(Long storeId, LocalDate from, LocalDate to) {
        Long store = scope(storeId);
        checkRange(from, to);
        return reports.byCashier(store, from, to);
    }

    /** CSV export of the daily table (also used for the tax report: it contains the tax column). */
    public String dailyCsv(Long storeId, LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder("store_id,day,sales_count,gross,discount,tax,refund_count,refund_total\n");
        for (DailySales d : daily(storeId, from, to)) {
            csv.append(d.getStoreId()).append(',').append(d.getDay()).append(',').append(d.getSalesCount()).append(',')
                    .append(d.getGross()).append(',').append(d.getDiscountTotal()).append(',').append(d.getTaxTotal())
                    .append(',').append(d.getRefundCount()).append(',').append(d.getRefundTotal()).append('\n');
        }
        return csv.toString();
    }

    /** Store users only see their own store; head office may pass any store or none (= all). */
    private Long scope(Long requested) {
        return CurrentUser.effectiveStoreId(requested);
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.badRequest("Both 'from' and 'to' (yyyy-MM-dd) are required");
        }
        if (to.isBefore(from)) {
            throw ApiException.badRequest("'to' must not be before 'from'");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw ApiException.badRequest("The date range is limited to " + MAX_RANGE_DAYS + " days");
        }
    }
}

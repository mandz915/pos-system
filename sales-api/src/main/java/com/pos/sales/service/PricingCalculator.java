package com.pos.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.pos.common.util.Money;
import com.pos.sales.dto.SaleLine;

public final class PricingCalculator {

    private PricingCalculator() {
    }

    public record LineAmounts(BigDecimal discountTotal, BigDecimal taxAmount, BigDecimal lineTotal) {
    }

    public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total) {
    }

    public static LineAmounts line(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPerUnit,
                                   BigDecimal taxRate, boolean taxInclusive) {
        return lineWithDiscount(quantity, unitPrice, Money.round(discountPerUnit.multiply(quantity)), taxRate, taxInclusive);
    }

    public static LineAmounts lineWithDiscount(BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineDiscount,
                                               BigDecimal taxRate, boolean taxInclusive) {
        BigDecimal gross = Money.round(unitPrice.multiply(quantity));
        BigDecimal discount = Money.round(lineDiscount).min(gross);
        BigDecimal net = gross.subtract(discount);
        if (taxInclusive) {
            BigDecimal tax = Money.round(net.multiply(taxRate).divide(Money.HUNDRED.add(taxRate), 10, RoundingMode.HALF_UP));
            return new LineAmounts(discount, tax, net);
        }
        BigDecimal tax = Money.round(net.multiply(taxRate).divide(Money.HUNDRED, 10, RoundingMode.HALF_UP));
        return new LineAmounts(discount, tax, net.add(tax));
    }

    public static Totals totals(List<SaleLine> lines) {
        BigDecimal subtotal = Money.zero();
        BigDecimal discount = Money.zero();
        BigDecimal tax = Money.zero();
        BigDecimal total = Money.zero();
        for (SaleLine line : lines) {
            subtotal = subtotal.add(Money.round(line.getUnitPrice().multiply(line.getQuantity())));
            discount = discount.add(line.getDiscountTotal());
            tax = tax.add(line.getTaxAmount());
            total = total.add(line.getLineTotal());
        }
        return new Totals(subtotal, discount, tax, total);
    }
}

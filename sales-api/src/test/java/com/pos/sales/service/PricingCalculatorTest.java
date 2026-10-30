package com.pos.sales.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pos.sales.domain.SaleLine;
import com.pos.sales.service.PricingCalculator.LineAmounts;
import com.pos.sales.service.PricingCalculator.Totals;

class PricingCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void inclusiveTaxIsContainedInThePrice() {
        // 2 x 100.00, 10.00 off per unit, 12% VAT included: net 180.00, VAT = 180 * 12 / 112 = 19.29
        LineAmounts amounts = PricingCalculator.line(bd("2"), bd("100.00"), bd("10.00"), bd("12"), true);

        assertThat(amounts.discountTotal()).isEqualByComparingTo("20.00");
        assertThat(amounts.taxAmount()).isEqualByComparingTo("19.29");
        assertThat(amounts.lineTotal()).isEqualByComparingTo("180.00");
    }

    @Test
    void exclusiveTaxIsAddedOnTop() {
        LineAmounts amounts = PricingCalculator.line(bd("2"), bd("100.00"), bd("10.00"), bd("12"), false);

        assertThat(amounts.taxAmount()).isEqualByComparingTo("21.60");
        assertThat(amounts.lineTotal()).isEqualByComparingTo("201.60");
    }

    @Test
    void zeroRatedItemHasNoTax() {
        LineAmounts amounts = PricingCalculator.line(bd("3"), bd("25.50"), bd("0"), bd("0"), true);

        assertThat(amounts.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(amounts.lineTotal()).isEqualByComparingTo("76.50");
    }

    @Test
    void fractionalQuantityIsRoundedToCents() {
        // 0.333 kg x 99.99 = 33.296667 -> 33.30
        LineAmounts amounts = PricingCalculator.line(bd("0.333"), bd("99.99"), bd("0"), bd("0"), true);

        assertThat(amounts.lineTotal()).isEqualByComparingTo("33.30");
    }

    @Test
    void totalsSumTheLines() {
        SaleLine first = new SaleLine();
        first.setQuantity(bd("2"));
        first.setUnitPrice(bd("100.00"));
        first.setDiscountTotal(bd("20.00"));
        first.setTaxAmount(bd("19.29"));
        first.setLineTotal(bd("180.00"));
        SaleLine second = new SaleLine();
        second.setQuantity(bd("1"));
        second.setUnitPrice(bd("50.00"));
        second.setDiscountTotal(bd("0.00"));
        second.setTaxAmount(bd("5.36"));
        second.setLineTotal(bd("50.00"));

        Totals totals = PricingCalculator.totals(List.of(first, second));

        assertThat(totals.subtotal()).isEqualByComparingTo("250.00");
        assertThat(totals.discountTotal()).isEqualByComparingTo("20.00");
        assertThat(totals.taxTotal()).isEqualByComparingTo("24.65");
        assertThat(totals.total()).isEqualByComparingTo("230.00");
    }
}

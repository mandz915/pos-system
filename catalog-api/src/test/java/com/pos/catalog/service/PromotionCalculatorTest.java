package com.pos.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pos.catalog.domain.Promotion;
import com.pos.catalog.domain.PromotionType;

class PromotionCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private static Promotion promo(PromotionType type, String value, Long productId, Long categoryId) {
        Promotion p = new Promotion();
        p.setType(type);
        p.setValue(new BigDecimal(value));
        p.setProductId(productId);
        p.setCategoryId(categoryId);
        return p;
    }

    @Test
    void noPromotionMeansNoDiscount() {
        assertThat(PromotionCalculator.discountPerUnit(new BigDecimal("100.00"), 1L, 1L, List.of(), NOW))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void percentDiscount() {
        List<Promotion> promos = List.of(promo(PromotionType.PERCENT, "15", 1L, null));
        assertThat(PromotionCalculator.discountPerUnit(new BigDecimal("80.00"), 1L, 9L, promos, NOW))
                .isEqualByComparingTo("12.00");
    }

    @Test
    void bestOfSeveralApplicablePromotionsWins() {
        List<Promotion> promos = List.of(
                promo(PromotionType.PERCENT, "10", null, null),      // 10.00 on 100
                promo(PromotionType.FIXED, "25", null, 5L),         // 25.00, category 5
                promo(PromotionType.FIXED, "50", 2L, null));        // other product, ignored
        assertThat(PromotionCalculator.discountPerUnit(new BigDecimal("100.00"), 1L, 5L, promos, NOW))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void discountNeverExceedsPrice() {
        List<Promotion> promos = List.of(promo(PromotionType.FIXED, "500", null, null));
        assertThat(PromotionCalculator.discountPerUnit(new BigDecimal("40.00"), 1L, 1L, promos, NOW))
                .isEqualByComparingTo("40.00");
    }

    @Test
    void expiredOrFuturePromotionsAreIgnored() {
        Promotion expired = promo(PromotionType.FIXED, "10", null, null);
        expired.setEndsAt(NOW.minusSeconds(60));
        Promotion future = promo(PromotionType.FIXED, "10", null, null);
        future.setStartsAt(NOW.plusSeconds(60));
        Promotion inactive = promo(PromotionType.FIXED, "10", null, null);
        inactive.setActive(false);

        assertThat(PromotionCalculator.discountPerUnit(new BigDecimal("100.00"), 1L, 1L,
                List.of(expired, future, inactive), NOW)).isEqualByComparingTo("0.00");
    }
}

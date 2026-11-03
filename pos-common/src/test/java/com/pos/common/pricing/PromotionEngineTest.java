package com.pos.common.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.pos.common.pricing.PromotionEngine.LineDiscount;
import com.pos.common.pricing.PromotionRule.Type;

class PromotionEngineTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private static PromotionRule rule(String code, Type type, String value, Long productId, Long categoryId) {
        return new PromotionRule(1L, code, code, type, new BigDecimal(value), null, null, null, productId, categoryId,
                null, null, null, null, true);
    }

    private static PromotionRule with(PromotionRule r, Integer buy, Integer get, Integer minQty, Long storeId,
                                      String coupon) {
        return new PromotionRule(r.id(), r.code(), r.name(), r.type(), r.value(), buy, get, minQty, r.productId(),
                r.categoryId(), storeId, coupon, r.startsAt(), r.endsAt(), r.active());
    }

    private static LineDiscount discount(String price, String qty, List<PromotionRule> rules, Set<String> coupons) {
        return PromotionEngine.lineDiscount(new BigDecimal(price), new BigDecimal(qty), 1L, 5L, 1L, rules, coupons, NOW);
    }

    @Test
    void noPromotionMeansNoDiscount() {
        LineDiscount d = discount("100.00", "2", List.of(), Set.of());
        assertThat(d.total()).isEqualByComparingTo("0.00");
        assertThat(d.promotionCode()).isNull();
    }

    @Test
    void percentAppliesToTheWholeLine() {
        LineDiscount d = discount("80.00", "3", List.of(rule("P15", Type.PERCENT, "15", 1L, null)), Set.of());
        assertThat(d.total()).isEqualByComparingTo("36.00");
        assertThat(d.promotionCode()).isEqualTo("P15");
    }

    @Test
    void fixedIsPerUnitAndNeverExceedsTheLine() {
        assertThat(discount("100.00", "3", List.of(rule("F", Type.FIXED, "10", null, null)), Set.of()).total())
                .isEqualByComparingTo("30.00");
        assertThat(discount("40.00", "1", List.of(rule("BIG", Type.FIXED, "500", null, null)), Set.of()).total())
                .isEqualByComparingTo("40.00");
    }

    @Test
    void buyTwoGetOneFreeRepeatsPerGroup() {
        PromotionRule bogo = with(rule("B2G1", Type.BUY_X_GET_Y, "1", 1L, null), 2, 1, null, null, null);
        assertThat(discount("50.00", "2", List.of(bogo), Set.of()).total()).isEqualByComparingTo("0.00");
        assertThat(discount("50.00", "3", List.of(bogo), Set.of()).total()).isEqualByComparingTo("50.00");
        assertThat(discount("50.00", "7", List.of(bogo), Set.of()).total()).isEqualByComparingTo("100.00");
    }

    @Test
    void minimumQuantityMustBeReached() {
        PromotionRule bulk = with(rule("BULK", Type.PERCENT, "10", null, null), null, null, 5, null, null);
        assertThat(discount("10.00", "4", List.of(bulk), Set.of()).total()).isEqualByComparingTo("0.00");
        assertThat(discount("10.00", "5", List.of(bulk), Set.of()).total()).isEqualByComparingTo("5.00");
    }

    @Test
    void couponPromotionsNeedTheCodeEnteredInAnyCase() {
        PromotionRule coupon = with(rule("WELCOME", Type.PERCENT, "20", null, null), null, null, null, null, "welcome20");
        assertThat(discount("100.00", "1", List.of(coupon), Set.of()).total()).isEqualByComparingTo("0.00");
        assertThat(discount("100.00", "1", List.of(coupon), Set.of(" WELCOME20 ")).total()).isEqualByComparingTo("20.00");
    }

    @Test
    void storeScopedPromotionOnlyAppliesInThatStore() {
        PromotionRule storeOnly = with(rule("S2", Type.FIXED, "5", null, null), null, null, null, 2L, null);
        assertThat(discount("100.00", "1", List.of(storeOnly), Set.of()).total()).isEqualByComparingTo("0.00");
        PromotionRule storeOne = with(rule("S1", Type.FIXED, "5", null, null), null, null, null, 1L, null);
        assertThat(discount("100.00", "1", List.of(storeOne), Set.of()).total()).isEqualByComparingTo("5.00");
    }

    @Test
    void bestSinglePromotionWinsAndTheyDoNotStack() {
        List<PromotionRule> rules = List.of(
                rule("TEN", Type.PERCENT, "10", null, null),
                rule("CAT", Type.FIXED, "25", null, 5L),
                rule("OTHER", Type.FIXED, "50", 2L, null));
        LineDiscount d = discount("100.00", "1", rules, Set.of());
        assertThat(d.total()).isEqualByComparingTo("25.00");
        assertThat(d.promotionCode()).isEqualTo("CAT");
    }

    @Test
    void expiredFutureAndInactiveAreIgnored() {
        PromotionRule base = rule("X", Type.FIXED, "10", null, null);
        PromotionRule expired = new PromotionRule(1L, "E", "E", Type.FIXED, base.value(), null, null, null, null, null,
                null, null, null, NOW.minusSeconds(60), true);
        PromotionRule future = new PromotionRule(1L, "F", "F", Type.FIXED, base.value(), null, null, null, null, null,
                null, null, NOW.plusSeconds(60), null, true);
        PromotionRule off = new PromotionRule(1L, "O", "O", Type.FIXED, base.value(), null, null, null, null, null,
                null, null, null, null, false);
        assertThat(discount("100.00", "1", List.of(expired, future, off), Set.of()).total()).isEqualByComparingTo("0.00");
    }

    @Test
    void unitLookupShowsOnlyAutomaticStorewidePercentAndFixed() {
        PromotionRule coupon = with(rule("C", Type.FIXED, "50", null, null), null, null, null, null, "X");
        PromotionRule bogo = with(rule("B", Type.BUY_X_GET_Y, "1", null, null), 1, 1, null, null, null);
        PromotionRule auto = rule("A", Type.PERCENT, "10", null, null);
        assertThat(PromotionEngine.discountPerUnit(new BigDecimal("100.00"), 1L, 5L, List.of(coupon, bogo, auto), NOW))
                .isEqualByComparingTo("10.00");
    }
}

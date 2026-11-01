package com.pos.common.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure promotion logic. Per line, the single promotion with the largest discount wins (promotions do not stack),
 * which keeps receipts explainable and avoids accidental 100% off.
 */
public final class PromotionEngine {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PromotionEngine() {
    }

    /** The discount of one line and the promotion that produced it ({@code promotionCode} is null when none applied). */
    public record LineDiscount(BigDecimal total, String promotionCode) {
        public static LineDiscount none() {
            return new LineDiscount(BigDecimal.ZERO.setScale(2), null);
        }
    }

    /**
     * Quantity-aware discount for one sale line.
     *
     * @param coupons the coupon codes the cashier entered (any case); coupon promotions apply only if listed
     */
    public static LineDiscount lineDiscount(BigDecimal unitPrice, BigDecimal quantity, Long productId, Long categoryId,
                                            Long storeId, List<PromotionRule> rules, Set<String> coupons, Instant now) {
        BigDecimal gross = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal best = BigDecimal.ZERO;
        String bestCode = null;
        for (PromotionRule rule : rules) {
            if (qualifies(rule, productId, categoryId, storeId, quantity, coupons, now)) {
                continue;
            }
            BigDecimal discount = amount(rule, unitPrice, quantity, gross).min(gross);
            if (discount.compareTo(best) > 0) {
                best = discount;
                bestCode = rule.code();
            }
        }
        return bestCode == null ? LineDiscount.none() : new LineDiscount(best.setScale(2, RoundingMode.HALF_UP), bestCode);
    }

    /**
     * Per-unit discount of the automatic (no coupon, any store) promotions for a single unit; what a price lookup
     * shows. PERCENT and FIXED only, because "buy 2 get 1" has no meaning for one unit.
     */
    public static BigDecimal discountPerUnit(BigDecimal price, Long productId, Long categoryId,
                                             List<PromotionRule> rules, Instant now) {
        BigDecimal best = BigDecimal.ZERO;
        for (PromotionRule rule : rules) {
            if (rule.type() == PromotionRule.Type.BUY_X_GET_Y || rule.couponCode() != null || rule.storeId() != null
                    || qualifies(rule, productId, categoryId, null, BigDecimal.ONE, Set.of(), now)) {
                continue;
            }
            BigDecimal discount = amount(rule, price, BigDecimal.ONE, price);
            if (discount.compareTo(best) > 0) {
                best = discount;
            }
        }
        return best.min(price).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean qualifies(PromotionRule rule, Long productId, Long categoryId, Long storeId, BigDecimal quantity,
                             Set<String> coupons, Instant now) {
        if (!rule.active()) {
            return true;
        }
        if (rule.startsAt() != null && now.isBefore(rule.startsAt())) {
            return true;
        }
        if (rule.endsAt() != null && !now.isBefore(rule.endsAt())) {
            return true;
        }
        if (rule.storeId() != null && !rule.storeId().equals(storeId)) {
            return true;
        }
        if (rule.productId() != null && !rule.productId().equals(productId)) {
            return true;
        }
        if (rule.productId() == null && rule.categoryId() != null && !rule.categoryId().equals(categoryId)) {
            return true;
        }
        if (rule.minQty() != null && quantity.compareTo(BigDecimal.valueOf(rule.minQty())) < 0) {
            return true;
        }
        return rule.couponCode() != null && !isListed(rule.couponCode(), coupons);
    }

    private static boolean isListed(String couponCode, Set<String> coupons) {
        String wanted = couponCode.trim().toUpperCase(Locale.ROOT);
        return coupons.stream().anyMatch(c -> c != null && c.trim().toUpperCase(Locale.ROOT).equals(wanted));
    }

    private static BigDecimal amount(PromotionRule rule, BigDecimal unitPrice, BigDecimal quantity, BigDecimal gross) {
        switch (rule.type()) {
            case PERCENT:
                return gross.multiply(rule.value()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            case FIXED:
                return rule.value().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            case BUY_X_GET_Y:
                int group = (rule.buyQty() == null ? 0 : rule.buyQty()) + (rule.getQty() == null ? 0 : rule.getQty());
                if (group <= 0 || rule.getQty() == null || rule.getQty() <= 0) {
                    return BigDecimal.ZERO;
                }
                BigDecimal groups = quantity.divideToIntegralValue(BigDecimal.valueOf(group));
                return groups.multiply(BigDecimal.valueOf(rule.getQty())).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            default:
                return BigDecimal.ZERO;
        }
    }
}

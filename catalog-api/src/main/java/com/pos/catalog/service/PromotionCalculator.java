package com.pos.catalog.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.pos.catalog.domain.Promotion;
import com.pos.catalog.domain.PromotionType;

/** Pure logic that picks the best per-unit discount for a product. */
public final class PromotionCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PromotionCalculator() {
    }

    /**
     * @return the largest discount per unit among the promotions that are active at {@code now} and apply to the
     *         product (never more than the price), at two decimals
     */
    public static BigDecimal discountPerUnit(BigDecimal price, Long productId, Long categoryId,
                                             List<Promotion> promotions, Instant now) {
        BigDecimal best = BigDecimal.ZERO;
        for (Promotion promotion : promotions) {
            if (!isLive(promotion, now) || !appliesTo(promotion, productId, categoryId)) {
                continue;
            }
            BigDecimal discount = promotion.getType() == PromotionType.PERCENT
                    ? price.multiply(promotion.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                    : promotion.getValue();
            if (discount.compareTo(best) > 0) {
                best = discount;
            }
        }
        return best.min(price).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean isLive(Promotion promotion, Instant now) {
        if (!promotion.isActive()) {
            return false;
        }
        if (promotion.getStartsAt() != null && now.isBefore(promotion.getStartsAt())) {
            return false;
        }
        return promotion.getEndsAt() == null || now.isBefore(promotion.getEndsAt());
    }

    static boolean appliesTo(Promotion promotion, Long productId, Long categoryId) {
        if (promotion.getProductId() != null) {
            return promotion.getProductId().equals(productId);
        }
        if (promotion.getCategoryId() != null) {
            return promotion.getCategoryId().equals(categoryId);
        }
        return true;
    }
}

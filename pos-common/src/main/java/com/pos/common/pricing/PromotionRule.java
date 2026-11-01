package com.pos.common.pricing;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A promotion as the pricing engine sees it. catalog-api stores and serves these; sales-api reads them to price a
 * sale. Scope: one product, one category, or everything (both empty); optionally one store and/or a coupon code.
 *
 * @param type      PERCENT (value = % off), FIXED (value = amount off per unit) or BUY_X_GET_Y (buy {@code buyQty},
 *                  get {@code getQty} of the same product free, repeating)
 * @param minQty    the line quantity must reach this to qualify (null = 1)
 * @param storeId   null = every store
 * @param couponCode null = applies automatically; otherwise only when the cashier enters the code
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PromotionRule(
        Long id,
        String code,
        String name,
        Type type,
        BigDecimal value,
        Integer buyQty,
        Integer getQty,
        Integer minQty,
        Long productId,
        Long categoryId,
        Long storeId,
        String couponCode,
        Instant startsAt,
        Instant endsAt,
        boolean active) {

    public enum Type { PERCENT, FIXED, BUY_X_GET_Y }
}

package com.pos.common.pricing;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

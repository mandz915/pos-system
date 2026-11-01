package com.pos.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.pricing.PromotionRule;

import lombok.Data;

/**
 * A discount (percent, fixed per unit, or buy-X-get-Y). It applies to one product, to every product of a category, or to everything
 * when both {@code productId} and {@code categoryId} are empty.
 */
@Data
public class Promotion {
    private Long id;
    private String code;
    private String name;
    private PromotionType type;
    private BigDecimal value;
    private Long productId;
    private Long categoryId;
    /** BUY_X_GET_Y only: pay for buyQty, get getQty free. */
    private Integer buyQty;
    private Integer getQty;
    /** The line quantity must reach this to qualify (empty = 1). */
    private Integer minQty;
    /** Empty = every store. */
    private Long storeId;
    /** Empty = applies automatically; otherwise only when the cashier enters this code. */
    private String couponCode;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active = true;
    private Instant createdAt;

    public PromotionRule toRule() {
        return new PromotionRule(id, code, name, PromotionRule.Type.valueOf(type.name()), value, buyQty, getQty, minQty,
                productId, categoryId, storeId, couponCode, startsAt, endsAt, active);
    }
}

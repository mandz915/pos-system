package com.pos.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.common.pricing.PromotionRule;

import lombok.Data;

@Data
public class Promotion {
    private Long id;
    private String code;
    private String name;
    private PromotionType type;
    private BigDecimal value;
    private Long productId;
    private Long categoryId;

    private Integer buyQty;
    private Integer getQty;

    private Integer minQty;

    private Long storeId;

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

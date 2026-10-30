package com.pos.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

/**
 * A per-unit discount. It applies to one product, to every product of a category, or to everything
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
    private Instant startsAt;
    private Instant endsAt;
    private boolean active = true;
    private Instant createdAt;
}

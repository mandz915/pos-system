package com.pos.sales.client;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.pos.common.pricing.PromotionRule;

public interface CatalogClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TaxInfo(Long id, String code, BigDecimal rate, boolean inclusive) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductInfo(
            Long id,
            String sku,
            String name,
            Long categoryId,
            BigDecimal price,
            TaxInfo tax,
            BigDecimal taxRate,
            boolean taxInclusive,
            BigDecimal discountPerUnit,
            boolean active) {
    }

    List<PromotionRule> livePromotions();

    ProductInfo getProduct(Long productId);
}

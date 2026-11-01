package com.pos.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.catalog.domain.PromotionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record TaxRateRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin("0") BigDecimal rate,
            boolean inclusive,
            Boolean active) {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 100) String name,
            Long parentId) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 50) String sku,
            @Size(max = 64) String barcode,
            @NotBlank @Size(max = 200) String name,
            String description,
            Long categoryId,
            Long taxRateId,
            @Size(max = 20) String uom,
            @NotNull @DecimalMin("0") BigDecimal price,
            @DecimalMin("0") BigDecimal cost,
            @Size(max = 500) String imageUrl,
            Boolean active) {
    }

    /**
     * What clients and other services see. {@code cost} is only filled for managers/admins.
     * {@code taxInclusive} tells whether {@code price} already contains the tax. {@code tax} repeats the tax details
     * as one optional object (null when the product has no tax assigned).
     */
    public record ProductView(
            Long id,
            String sku,
            String barcode,
            String name,
            String description,
            Long categoryId,
            Long taxRateId,
            BigDecimal taxRate,
            boolean taxInclusive,
            String uom,
            BigDecimal price,
            BigDecimal cost,
            BigDecimal discountPerUnit,
            BigDecimal effectivePrice,
            String imageUrl,
            boolean active,
            TaxInfo tax) {

        /** The same view without the cost, safe to cache and to show to any signed-in user. */
        public ProductView withoutCost() {
            return new ProductView(id, sku, barcode, name, description, categoryId, taxRateId, taxRate, taxInclusive,
                    uom, price, null, discountPerUnit, effectivePrice, imageUrl, active, tax);
        }
    }

    /** The tax that applies to an item; {@code null} on a view when the product has no tax assigned (untaxed). */
    public record TaxInfo(Long id, String code, BigDecimal rate, boolean inclusive) {
    }

    public record PromotionRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull PromotionType type,
            @DecimalMin(value = "0.01") BigDecimal value,
            @Min(1) Integer buyQty,
            @Min(1) Integer getQty,
            @Min(1) Integer minQty,
            Long productId,
            Long categoryId,
            Long storeId,
            @Size(max = 30) String couponCode,
            Instant startsAt,
            Instant endsAt,
            Boolean active) {
    }
}

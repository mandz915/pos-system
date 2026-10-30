package com.pos.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import com.pos.catalog.domain.PromotionType;

import jakarta.validation.constraints.DecimalMin;
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
     * {@code taxInclusive} tells whether {@code price} already contains the tax.
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
            boolean active) {
    }

    public record PromotionRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull PromotionType type,
            @NotNull @DecimalMin(value = "0.01") BigDecimal value,
            Long productId,
            Long categoryId,
            Instant startsAt,
            Instant endsAt,
            Boolean active) {
    }
}

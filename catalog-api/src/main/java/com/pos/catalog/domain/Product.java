package com.pos.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class Product {
    private Long id;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private Long categoryId;
    private Long taxRateId;
    private String uom = "pc";
    private BigDecimal price;
    private BigDecimal cost = BigDecimal.ZERO;
    private String imageUrl;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    // Filled by the queries that join tax_rates (read only).
    private BigDecimal taxRate;
    private Boolean taxInclusive;
}

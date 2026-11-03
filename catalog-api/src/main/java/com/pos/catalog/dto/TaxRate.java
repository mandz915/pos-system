package com.pos.catalog.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TaxRate {
    private Long id;
    private String code;
    private String name;

    private BigDecimal rate;

    private boolean inclusive = true;
    private boolean active = true;
}

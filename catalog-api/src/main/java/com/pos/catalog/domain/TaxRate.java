package com.pos.catalog.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TaxRate {
    private Long id;
    private String code;
    private String name;
    /** Percentage, e.g. 12.00 for 12%. */
    private BigDecimal rate;
    /** True when shelf prices already include this tax. */
    private boolean inclusive = true;
    private boolean active = true;
}

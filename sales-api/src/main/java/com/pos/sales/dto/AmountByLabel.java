package com.pos.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AmountByLabel {
    private String label;
    private BigDecimal amount;
}

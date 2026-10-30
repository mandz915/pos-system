package com.pos.sales.domain;

import java.math.BigDecimal;

import lombok.Data;

/** Query result: an amount grouped by a label (payment method, cash movement type...). */
@Data
public class AmountByLabel {
    private String label;
    private BigDecimal amount;
}

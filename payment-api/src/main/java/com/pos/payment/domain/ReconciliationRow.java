package com.pos.payment.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ReconciliationRow {
    private String method;
    private String kind;
    private String status;
    private long count;
    private BigDecimal total;
}

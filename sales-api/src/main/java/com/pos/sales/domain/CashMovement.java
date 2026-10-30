package com.pos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class CashMovement {
    private Long id;
    private Long shiftId;
    private CashMovementType type;
    private BigDecimal amount;
    private String reason;
    private Long createdBy;
    private Instant createdAt;
}

package com.pos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class Shift {
    private Long id;
    private Long storeId;
    private Long terminalId;
    private Long cashierId;
    private ShiftStatus status;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private BigDecimal expectedCash;
    private Instant openedAt;
    private Instant closedAt;
}

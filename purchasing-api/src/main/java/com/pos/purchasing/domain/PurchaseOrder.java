package com.pos.purchasing.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class PurchaseOrder {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private Long storeId;
    private PoStatus status;
    private String note;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private Long createdBy;
    private Instant createdAt;
    private Instant orderedAt;
    private Instant closedAt;
}

package com.pos.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class CreditTransaction {
    private Long id;
    private Long customerId;

    private String type;
    private BigDecimal amount;

    private BigDecimal balance;
    private String refType;
    private String refId;
    private String note;
    private Instant createdAt;
}

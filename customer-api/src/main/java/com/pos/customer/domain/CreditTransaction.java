package com.pos.customer.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class CreditTransaction {
    private Long id;
    private Long customerId;
    /** CHARGE (customer owes more) or PAYMENT (customer pays down the balance). */
    private String type;
    private BigDecimal amount;
    /** Amount owed after this transaction. */
    private BigDecimal balance;
    private String refType;
    private String refId;
    private String note;
    private Instant createdAt;
}

package com.pos.customer.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class LoyaltyTransaction {
    private Long id;
    private Long customerId;

    private String type;

    private long points;
    private long balance;
    private String refType;
    private String refId;
    private String note;
    private Instant createdAt;
}

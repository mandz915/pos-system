package com.pos.customer.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class LoyaltyTransaction {
    private Long id;
    private Long customerId;
    /** EARN, REDEEM, REVERSE or ADJUST. */
    private String type;
    /** Signed: positive adds points, negative removes them. */
    private long points;
    private long balance;
    private String refType;
    private String refId;
    private String note;
    private Instant createdAt;
}

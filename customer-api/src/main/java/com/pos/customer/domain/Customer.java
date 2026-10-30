package com.pos.customer.domain;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class Customer {
    private Long id;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String address;
    private long pointsBalance;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private BigDecimal creditBalance = BigDecimal.ZERO;
    private boolean active = true;
    private Instant createdAt;
}

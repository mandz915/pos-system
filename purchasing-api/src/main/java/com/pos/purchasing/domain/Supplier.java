package com.pos.purchasing.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class Supplier {
    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private boolean active = true;
    private Instant createdAt;
}

package com.pos.user.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class Tenant {

    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";

    private Long id;
    private String code;
    private String name;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isActive() {
        return ACTIVE.equals(status);
    }
}

package com.pos.user.domain;

import java.time.Instant;

import lombok.Data;

/** A client company. Every store, user, product and sale belongs to exactly one tenant. */
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

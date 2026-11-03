package com.pos.user.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String pinHash;
    private String fullName;
    private String email;
    private Long storeId;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}

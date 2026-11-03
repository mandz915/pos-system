package com.pos.administration.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class Store {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String tin;
    private boolean active = true;
    private Instant createdAt;
}

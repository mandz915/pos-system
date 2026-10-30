package com.pos.administration.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class Setting {
    /** 0 = global default. */
    private Long storeId;
    private String settingKey;
    private String settingValue;
    private Instant updatedAt;
}

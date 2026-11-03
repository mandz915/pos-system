package com.pos.administration.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class Setting {

    private Long storeId;
    private String settingKey;
    private String settingValue;
    private Instant updatedAt;
}

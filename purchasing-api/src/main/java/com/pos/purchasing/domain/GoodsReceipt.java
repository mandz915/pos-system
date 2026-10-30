package com.pos.purchasing.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class GoodsReceipt {
    private Long id;
    private Long poId;
    private String note;
    private Long receivedBy;
    private Instant receivedAt;
}

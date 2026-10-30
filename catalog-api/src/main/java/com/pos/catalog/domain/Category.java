package com.pos.catalog.domain;

import lombok.Data;

@Data
public class Category {
    private Long id;
    private String name;
    private Long parentId;
    private boolean active = true;
}

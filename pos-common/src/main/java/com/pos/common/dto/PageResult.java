package com.pos.common.dto;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResult<T> of(List<T> items, Paging paging, long total) {
        return new PageResult<>(items, paging.page(), paging.size(), total);
    }
}

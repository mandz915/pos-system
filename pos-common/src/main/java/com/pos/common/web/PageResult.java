package com.pos.common.web;

import java.util.List;

/** A page of results plus the total number of matching rows. */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResult<T> of(List<T> items, Paging paging, long total) {
        return new PageResult<>(items, paging.page(), paging.size(), total);
    }
}

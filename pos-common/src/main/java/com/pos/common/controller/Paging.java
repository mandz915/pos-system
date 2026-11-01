package com.pos.common.controller;

/** Normalised paging parameters (zero-based page, size between 1 and 200). */
public record Paging(int page, int size) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public static Paging of(Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
        return new Paging(p, s);
    }

    public int offset() {
        return page * size;
    }
}

package com.pos.common.cache;

public final class CacheNames {

    public static final String CATALOG = "catalog";

    private CacheNames() {
    }

    public static String productById(long id) {
        return "product:" + id;
    }

    public static String productByBarcode(String barcode) {
        return "barcode:" + barcode;
    }

    public static String productBySku(String sku) {
        return "sku:" + sku;
    }
}

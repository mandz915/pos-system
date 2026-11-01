package com.pos.common.cache;

/**
 * Cache namespaces and key builders shared by the service that fills a cache and the services that read it.
 * catalog-api owns the catalog namespace (fills and evicts it); sales-api reads it to skip an HTTP hop per line.
 */
public final class CacheNames {

    /** Values are catalog product views WITHOUT cost, so they are safe to hand to any signed-in user. */
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

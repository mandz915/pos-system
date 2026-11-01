package com.pos.common.tenant;

/** Names shared by the event producer and consumers. */
public final class TenantHeaders {

    /** Kafka record header carrying the tenant id (decimal text) of the event. */
    public static final String KAFKA = "pos-tenant";

    private TenantHeaders() {
    }
}

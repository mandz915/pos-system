package com.pos.common.security;

/** Role names carried in the JWT. */
public final class Roles {

    /** Platform operator: manages client companies (tenants), has no access to their business data. */
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    /** Administrator of one client company. */
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String CASHIER = "CASHIER";

    private Roles() {
    }
}

package com.pos.customer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure loyalty maths so it can be unit tested without a database. */
public final class LoyaltyCalculator {

    private LoyaltyCalculator() {
    }

    /** Whole points earned for an amount spent (rounded down): {@code floor(amount * pointsPerCurrency)}. */
    public static long pointsFor(BigDecimal amount, BigDecimal pointsPerCurrency) {
        if (amount == null || amount.signum() <= 0 || pointsPerCurrency == null || pointsPerCurrency.signum() <= 0) {
            return 0;
        }
        return amount.multiply(pointsPerCurrency).setScale(0, RoundingMode.DOWN).longValueExact();
    }

    /** Points to take back for a refund; never more than the customer currently has. */
    public static long pointsToReverse(BigDecimal refundAmount, BigDecimal pointsPerCurrency, long currentBalance) {
        return Math.min(pointsFor(refundAmount, pointsPerCurrency), Math.max(0, currentBalance));
    }
}

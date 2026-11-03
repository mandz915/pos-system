package com.pos.customer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class LoyaltyCalculator {

    private LoyaltyCalculator() {
    }

    public static long pointsFor(BigDecimal amount, BigDecimal pointsPerCurrency) {
        if (amount == null || amount.signum() <= 0 || pointsPerCurrency == null || pointsPerCurrency.signum() <= 0) {
            return 0;
        }
        return amount.multiply(pointsPerCurrency).setScale(0, RoundingMode.DOWN).longValueExact();
    }

    public static long pointsToReverse(BigDecimal refundAmount, BigDecimal pointsPerCurrency, long currentBalance) {
        return Math.min(pointsFor(refundAmount, pointsPerCurrency), Math.max(0, currentBalance));
    }
}

package com.pos.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money helpers: every amount is kept at two decimals, rounded half-up. */
public final class Money {

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    }
}

package com.pos.customer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LoyaltyCalculatorTest {

    private static final BigDecimal ONE_POINT_PER_100 = new BigDecimal("0.01");

    @Test
    void earnsWholePointsRoundedDown() {
        assertThat(LoyaltyCalculator.pointsFor(new BigDecimal("1234.99"), ONE_POINT_PER_100)).isEqualTo(12);
        assertThat(LoyaltyCalculator.pointsFor(new BigDecimal("99.99"), ONE_POINT_PER_100)).isZero();
    }

    @Test
    void noPointsForZeroOrNegativeAmount() {
        assertThat(LoyaltyCalculator.pointsFor(BigDecimal.ZERO, ONE_POINT_PER_100)).isZero();
        assertThat(LoyaltyCalculator.pointsFor(new BigDecimal("-50"), ONE_POINT_PER_100)).isZero();
    }

    @Test
    void noPointsWhenRateIsNotPositive() {
        assertThat(LoyaltyCalculator.pointsFor(new BigDecimal("500"), BigDecimal.ZERO)).isZero();
    }

    @Test
    void reversalNeverTakesMoreThanTheBalance() {
        assertThat(LoyaltyCalculator.pointsToReverse(new BigDecimal("1000"), ONE_POINT_PER_100, 4)).isEqualTo(4);
        assertThat(LoyaltyCalculator.pointsToReverse(new BigDecimal("1000"), ONE_POINT_PER_100, 50)).isEqualTo(10);
        assertThat(LoyaltyCalculator.pointsToReverse(new BigDecimal("1000"), ONE_POINT_PER_100, 0)).isZero();
    }
}

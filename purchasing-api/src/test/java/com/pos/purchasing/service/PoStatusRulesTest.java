package com.pos.purchasing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pos.purchasing.domain.PoLine;
import com.pos.purchasing.domain.PoStatus;

class PoStatusRulesTest {

    private static PoLine line(String ordered, String received) {
        PoLine line = new PoLine();
        line.setOrderedQty(new BigDecimal(ordered));
        line.setReceivedQty(new BigDecimal(received));
        return line;
    }

    @Test
    void fullyReceivedOrderIsReceived() {
        assertThat(PoStatusRules.afterReceiving(List.of(line("10", "10"), line("5", "5")))).isEqualTo(PoStatus.RECEIVED);
    }

    @Test
    void anyOutstandingLineMeansPartial() {
        assertThat(PoStatusRules.afterReceiving(List.of(line("10", "10"), line("5", "2")))).isEqualTo(PoStatus.PARTIAL);
    }

    @Test
    void receivingIsOnlyPossibleOnceOrdered() {
        assertThat(PoStatusRules.canReceive(PoStatus.DRAFT)).isFalse();
        assertThat(PoStatusRules.canReceive(PoStatus.ORDERED)).isTrue();
        assertThat(PoStatusRules.canReceive(PoStatus.PARTIAL)).isTrue();
        assertThat(PoStatusRules.canReceive(PoStatus.RECEIVED)).isFalse();
        assertThat(PoStatusRules.canReceive(PoStatus.CANCELLED)).isFalse();
    }

    @Test
    void cancellingIsBlockedOnceGoodsArrived() {
        assertThat(PoStatusRules.canCancel(PoStatus.DRAFT)).isTrue();
        assertThat(PoStatusRules.canCancel(PoStatus.ORDERED)).isTrue();
        assertThat(PoStatusRules.canCancel(PoStatus.PARTIAL)).isFalse();
        assertThat(PoStatusRules.canCancel(PoStatus.RECEIVED)).isFalse();
    }
}

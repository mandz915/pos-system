package com.pos.purchasing.service;

import java.util.List;

import com.pos.purchasing.domain.PoLine;
import com.pos.purchasing.domain.PoStatus;

/** Pure status rules, unit tested without a database. */
public final class PoStatusRules {

    private PoStatusRules() {
    }

    /** RECEIVED when every line is fully received, otherwise PARTIAL. */
    public static PoStatus afterReceiving(List<PoLine> lines) {
        boolean complete = lines.stream().allMatch(l -> l.outstanding().signum() <= 0);
        return complete ? PoStatus.RECEIVED : PoStatus.PARTIAL;
    }

    public static boolean canReceive(PoStatus status) {
        return status == PoStatus.ORDERED || status == PoStatus.PARTIAL;
    }

    /** Only an order that has not received anything yet can be cancelled. */
    public static boolean canCancel(PoStatus status) {
        return status == PoStatus.DRAFT || status == PoStatus.ORDERED;
    }
}

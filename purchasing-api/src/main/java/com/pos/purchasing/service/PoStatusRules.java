package com.pos.purchasing.service;

import java.util.List;

import com.pos.purchasing.dto.PoLine;
import com.pos.purchasing.dto.PoStatus;

public final class PoStatusRules {

    private PoStatusRules() {
    }

    public static PoStatus afterReceiving(List<PoLine> lines) {
        boolean complete = lines.stream().allMatch(l -> l.outstanding().signum() <= 0);
        return complete ? PoStatus.RECEIVED : PoStatus.PARTIAL;
    }

    public static boolean canReceive(PoStatus status) {
        return status == PoStatus.ORDERED || status == PoStatus.PARTIAL;
    }

    public static boolean canCancel(PoStatus status) {
        return status == PoStatus.DRAFT || status == PoStatus.ORDERED;
    }
}

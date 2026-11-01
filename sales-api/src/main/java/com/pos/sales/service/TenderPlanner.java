package com.pos.sales.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.common.util.Money;
import com.pos.sales.dto.SalesDtos.PaymentLine;

/**
 * Turns what the customer offers into what is credited to the sale. Non-cash tenders must be exact (they can never
 * exceed the balance); cash is applied last and any excess becomes change.
 */
public final class TenderPlanner {

    private TenderPlanner() {
    }

    /** @param applied credited to the sale; @param tendered handed over by the customer */
    public record Tender(PaymentMethod method, BigDecimal applied, BigDecimal tendered, String reference) {
    }

    public record Plan(List<Tender> tenders, BigDecimal total, BigDecimal paidTotal, BigDecimal change) {
    }

    public static Plan plan(BigDecimal total, List<PaymentLine> offered) {
        if (total.signum() <= 0) {
            throw ApiException.badRequest("Nothing to pay: the sale total is zero");
        }
        BigDecimal remaining = total;
        BigDecimal cashTendered = Money.zero();
        BigDecimal cashApplied = Money.zero();
        List<Tender> tenders = new ArrayList<>();

        for (PaymentLine line : offered) {
            if (line.method() == PaymentMethod.CASH) {
                continue;
            }
            BigDecimal amount = Money.round(line.amount());
            if (amount.compareTo(remaining) > 0) {
                throw ApiException.badRequest(line.method() + " payment of " + amount
                        + " exceeds the balance due of " + remaining);
            }
            remaining = remaining.subtract(amount);
            tenders.add(new Tender(line.method(), amount, amount, line.reference()));
        }

        for (PaymentLine line : offered) {
            if (line.method() != PaymentMethod.CASH) {
                continue;
            }
            if (remaining.signum() == 0) {
                throw ApiException.badRequest("Cash is not needed: the balance is already covered");
            }
            BigDecimal tendered = Money.round(line.amount());
            BigDecimal applied = tendered.min(remaining);
            remaining = remaining.subtract(applied);
            cashTendered = cashTendered.add(tendered);
            cashApplied = cashApplied.add(applied);
            tenders.add(new Tender(PaymentMethod.CASH, applied, tendered, line.reference()));
        }

        if (remaining.signum() > 0) {
            throw ApiException.badRequest("Insufficient payment: " + remaining + " still due");
        }
        BigDecimal change = cashTendered.subtract(cashApplied);
        return new Plan(List.copyOf(tenders), total, total.add(change), change);
    }
}

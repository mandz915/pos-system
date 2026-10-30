package com.pos.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.sales.service.TenderPlanner.Plan;
import com.pos.sales.web.SalesDtos.PaymentLine;

class TenderPlannerTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static PaymentLine pay(PaymentMethod method, String amount) {
        return new PaymentLine(method, bd(amount), null);
    }

    @Test
    void exactCardPayment() {
        Plan plan = TenderPlanner.plan(bd("180.00"), List.of(pay(PaymentMethod.CARD, "180.00")));

        assertThat(plan.change()).isEqualByComparingTo("0.00");
        assertThat(plan.paidTotal()).isEqualByComparingTo("180.00");
        assertThat(plan.tenders()).hasSize(1);
    }

    @Test
    void cashOverpaymentGivesChange() {
        Plan plan = TenderPlanner.plan(bd("180.00"), List.of(pay(PaymentMethod.CASH, "200.00")));

        assertThat(plan.tenders().get(0).applied()).isEqualByComparingTo("180.00");
        assertThat(plan.tenders().get(0).tendered()).isEqualByComparingTo("200.00");
        assertThat(plan.change()).isEqualByComparingTo("20.00");
        assertThat(plan.paidTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void splitPaymentCardThenCashWithChange() {
        // Cash is offered first but applied last, after the (exact) card amount.
        Plan plan = TenderPlanner.plan(bd("500.00"),
                List.of(pay(PaymentMethod.CASH, "300.00"), pay(PaymentMethod.CARD, "250.00")));

        assertThat(plan.tenders().get(0).method()).isEqualTo(PaymentMethod.CARD);
        assertThat(plan.tenders().get(1).applied()).isEqualByComparingTo("250.00");
        assertThat(plan.change()).isEqualByComparingTo("50.00");
    }

    @Test
    void cardCannotExceedBalance() {
        assertThatThrownBy(() -> TenderPlanner.plan(bd("100.00"), List.of(pay(PaymentMethod.CARD, "150.00"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void insufficientPaymentIsRejected() {
        assertThatThrownBy(() -> TenderPlanner.plan(bd("100.00"), List.of(pay(PaymentMethod.CASH, "60.00"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void cashIsRejectedWhenAlreadyCovered() {
        assertThatThrownBy(() -> TenderPlanner.plan(bd("100.00"),
                List.of(pay(PaymentMethod.CARD, "100.00"), pay(PaymentMethod.CASH, "20.00"))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void zeroTotalCannotBePaid() {
        assertThatThrownBy(() -> TenderPlanner.plan(bd("0.00"), List.of(pay(PaymentMethod.CASH, "10.00"))))
                .isInstanceOf(ApiException.class);
    }
}

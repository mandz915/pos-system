package com.pos.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.common.pricing.PromotionRule;
import com.pos.common.pricing.PromotionRule.Type;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.sales.client.CatalogClient;
import com.pos.sales.client.CatalogClient.ProductInfo;
import com.pos.sales.client.CatalogClient.TaxInfo;
import com.pos.sales.client.PaymentClient;
import com.pos.sales.client.PaymentClient.PaymentResult;
import com.pos.sales.dto.Sale;
import com.pos.sales.dto.SaleLine;
import com.pos.sales.dto.SaleStatus;
import com.pos.sales.dto.Shift;
import com.pos.sales.dto.ShiftStatus;
import com.pos.sales.mapper.ReturnMapper;
import com.pos.sales.mapper.SaleLineMapper;
import com.pos.sales.mapper.SaleMapper;
import com.pos.sales.mapper.SalePaymentMapper;
import com.pos.sales.mapper.ShiftMapper;
import com.pos.sales.service.TenderPlanner.Tender;
import com.pos.sales.dto.SalesDtos.CheckoutRequest;
import com.pos.sales.dto.SalesDtos.CreateSaleRequest;
import com.pos.sales.dto.SalesDtos.LineRequest;
import com.pos.sales.dto.SalesDtos.LineTax;
import com.pos.sales.dto.SalesDtos.PaymentLine;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleServiceTest {

    @Mock
    private SaleMapper sales;
    @Mock
    private SaleLineMapper lineMapper;
    @Mock
    private SalePaymentMapper paymentMapper;
    @Mock
    private ReturnMapper returns;
    @Mock
    private ShiftMapper shifts;
    @Mock
    private SaleTxService tx;
    @Mock
    private CatalogClient catalog;
    @Mock
    private PaymentClient payments;

    private SaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SaleServiceImpl(sales, lineMapper, paymentMapper, returns, shifts, tx, catalog, payments);
        asCashier();

        Shift shift = new Shift();
        shift.setId(3L);
        shift.setStoreId(1L);
        shift.setStatus(ShiftStatus.OPEN);
        when(shifts.findOpenByTerminal(1L)).thenReturn(shift);
        when(shifts.findById(3L)).thenReturn(shift);

        when(catalog.getProduct(5L)).thenReturn(new ProductInfo(5L, "COFFEE", "Brewed Coffee", 5L,
                new BigDecimal("100.00"), new TaxInfo(1L, "VAT12", new BigDecimal("12.00"), true),
                new BigDecimal("12.00"), true, BigDecimal.ZERO, true));
        when(catalog.livePromotions()).thenReturn(List.of(
                rule("TEN", "10", null),
                rule("WELCOME", "15", "welcome20")));

        when(tx.createDraft(any(), any(), anyLong(), any())).thenReturn(77L);
        Sale saved = new Sale();
        saved.setId(77L);
        saved.setStoreId(1L);
        saved.setCashierId(10L);
        when(sales.findById(77L)).thenReturn(saved);
    }

    @AfterEach
    void clear() {
        CurrentUser.clear();
    }

    private static void asCashier() {
        CurrentUser.set(new AuthenticatedUser(10L, "cora", Set.of(Roles.CASHIER), 1L, 1L), "token");
    }

    private static PromotionRule rule(String code, String value, String coupon) {

        Type type = coupon == null ? Type.PERCENT : Type.FIXED;
        return new PromotionRule(1L, code, code, type, new BigDecimal(value), null, null, null, null, null, null,
                coupon, null, null, true);
    }

    private CreateSaleRequest request(List<String> coupons, LineRequest... lines) {
        return new CreateSaleRequest("ref-1", 1L, 1L, null, List.of(lines), coupons);
    }

    private SaleLine createdLine() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SaleLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(tx).createDraft(any(), any(), anyLong(), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        return captor.getValue().get(0);
    }

    @Test
    void creatingTheSameClientRefTwiceReturnsTheFirstSaleWithoutCreatingAnother() {
        Sale existing = new Sale();
        existing.setId(77L);
        existing.setStoreId(1L);
        when(sales.findByClientRef("ref-1")).thenReturn(existing);

        service.create(request(null, new LineRequest(5L, BigDecimal.ONE, null)));

        verify(tx, never()).createDraft(any(), any(), anyLong(), any());
        verifyNoInteractions(catalog);
    }

    @Test
    void aSaleNeedsAnOpenShiftOnTheTerminal() {
        when(shifts.findOpenByTerminal(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(request(null, new LineRequest(5L, BigDecimal.ONE, null))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No open shift");
    }

    @Test
    void theBestAutomaticPromotionIsAppliedAndTaxIsTakenFromTheProduct() {
        service.create(request(null, new LineRequest(5L, new BigDecimal("2"), null)));

        SaleLine line = createdLine();
        assertThat(line.getDiscountTotal()).isEqualByComparingTo("20.00");
        assertThat(line.getPromotionCode()).isEqualTo("TEN");
        assertThat(line.getTaxCode()).isEqualTo("VAT12");
        assertThat(line.isTaxOverridden()).isFalse();
        assertThat(line.getLineTotal()).isEqualByComparingTo("180.00");
        assertThat(line.getTaxAmount()).isEqualByComparingTo("19.29");
    }

    @Test
    void aCouponPromotionOnlyAppliesWhenTheCashierEntersItAndTheBestOfferWins() {
        service.create(request(List.of("WELCOME20"), new LineRequest(5L, new BigDecimal("2"), null)));

        SaleLine line = createdLine();
        assertThat(line.getPromotionCode()).isEqualTo("WELCOME");
        assertThat(line.getDiscountTotal()).isEqualByComparingTo("30.00");
        assertThat(line.getLineTotal()).isEqualByComparingTo("170.00");
    }

    @Test
    void anUnknownCouponIsRejectedAndNothingIsSaved() {
        assertThatThrownBy(() -> service.create(request(List.of("NOPE"), new LineRequest(5L, BigDecimal.ONE, null))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Coupon 'NOPE' is not valid");

        verify(tx, never()).createDraft(any(), any(), anyLong(), any());
    }

    @Test
    void aCashierCannotOverrideTheTaxOfALine() {
        LineRequest withTax = new LineRequest(5L, BigDecimal.ONE, new LineTax("EXEMPT", BigDecimal.ZERO, null));

        assertThatThrownBy(() -> service.create(request(null, withTax)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(tx, never()).createDraft(any(), any(), anyLong(), any());
    }

    @Test
    void aSupervisorCanMakeALineTaxExemptAndItIsRecordedAsAnOverride() {
        CurrentUser.set(new AuthenticatedUser(11L, "sam", Set.of(Roles.SUPERVISOR), 1L, 1L), "token");
        LineRequest exempt = new LineRequest(5L, new BigDecimal("2"), new LineTax("EXEMPT", BigDecimal.ZERO, null));

        service.create(request(null, exempt));

        SaleLine line = createdLine();
        assertThat(line.isTaxOverridden()).isTrue();
        assertThat(line.getTaxCode()).isEqualTo("EXEMPT");
        assertThat(line.getTaxRate()).isEqualByComparingTo("0");
        assertThat(line.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(line.getLineTotal()).isEqualByComparingTo("180.00");
    }

    @Test
    void aTaxObjectCanSwitchALineToTaxExclusive() {
        CurrentUser.set(new AuthenticatedUser(11L, "sam", Set.of(Roles.SUPERVISOR), 1L, 1L), "token");
        LineRequest exclusive = new LineRequest(5L, BigDecimal.ONE, new LineTax("SERVICE", new BigDecimal("10"), false));
        when(catalog.livePromotions()).thenReturn(List.of());

        service.create(request(null, exclusive));

        SaleLine line = createdLine();
        assertThat(line.isTaxInclusive()).isFalse();
        assertThat(line.getTaxAmount()).isEqualByComparingTo("10.00");
        assertThat(line.getLineTotal()).isEqualByComparingTo("110.00");
    }

    @Test
    void aProductThatIsNotForSaleCannotBeAdded() {
        when(catalog.getProduct(6L)).thenReturn(new ProductInfo(6L, "OLD", "Discontinued", 5L,
                new BigDecimal("10.00"), null, BigDecimal.ZERO, true, BigDecimal.ZERO, false));

        assertThatThrownBy(() -> service.create(request(null, new LineRequest(6L, BigDecimal.ONE, null))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not for sale");
    }

    private void openSaleOf(String total) {
        Sale sale = new Sale();
        sale.setId(5L);
        sale.setStoreId(1L);
        sale.setShiftId(3L);
        sale.setCashierId(10L);
        sale.setStatus(SaleStatus.OPEN);
        sale.setTotal(new BigDecimal(total));
        when(sales.findById(5L)).thenReturn(sale);
    }

    private static CheckoutRequest ewalletThenCard() {
        return new CheckoutRequest(List.of(
                new PaymentLine(PaymentMethod.EWALLET, new BigDecimal("30.00"), "gc-1"),
                new PaymentLine(PaymentMethod.CARD, new BigDecimal("70.00"), "visa-1")));
    }

    @Test
    void whenALaterPaymentIsDeclinedTheEarlierApprovedOnesAreReversed() {
        openSaleOf("100.00");
        when(payments.charge(eq(5L), eq(1L), any(Tender.class))).thenAnswer(invocation -> {
            Tender tender = invocation.getArgument(2);
            return tender.method() == PaymentMethod.EWALLET
                    ? new PaymentResult(1L, "APPROVED", "g1", null)
                    : new PaymentResult(2L, "DECLINED", null, "insufficient funds");
        });

        assertThatThrownBy(() -> service.checkout(5L, ewalletThenCard()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("CARD payment was declined");

        verify(payments).reverse(1L);
        verify(payments, never()).reverse(2L);
        verify(tx, never()).complete(any(), any(), any());
    }

    @Test
    void whenTheDatabaseStepFailsEveryApprovedPaymentIsReversed() {
        openSaleOf("100.00");
        when(payments.charge(eq(5L), eq(1L), any(Tender.class))).thenAnswer(invocation -> {
            Tender tender = invocation.getArgument(2);
            return new PaymentResult(tender.method() == PaymentMethod.EWALLET ? 1L : 2L, "APPROVED", "g", null);
        });
        doThrow(ApiException.conflict("The sale changed while it was being paid; please retry"))
                .when(tx).complete(any(), any(), any());

        assertThatThrownBy(() -> service.checkout(5L, ewalletThenCard())).isInstanceOf(ApiException.class);

        verify(payments).reverse(1L);
        verify(payments).reverse(2L);
    }

    @Test
    void aSuccessfulCheckoutCompletesTheSaleOnceAndReversesNothing() {
        openSaleOf("100.00");
        when(payments.charge(eq(5L), eq(1L), any(Tender.class)))
                .thenReturn(new PaymentResult(1L, "APPROVED", "g", null));

        service.checkout(5L, ewalletThenCard());

        verify(tx).complete(eq(5L), any(), any());
        verify(payments, never()).reverse(anyLong());
    }

    @Test
    void anotherCashiersSaleCannotBeCheckedOutByACashier() {
        openSaleOf("100.00");
        CurrentUser.set(new AuthenticatedUser(99L, "other", Set.of(Roles.CASHIER), 1L, 1L), "token");

        assertThatThrownBy(() -> service.checkout(5L, ewalletThenCard()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(payments);
    }

    @Test
    void aSaleThatIsNotOpenCannotBePaid() {
        openSaleOf("100.00");
        when(sales.findById(5L)).thenAnswer(invocation -> {
            Sale done = new Sale();
            done.setId(5L);
            done.setStoreId(1L);
            done.setCashierId(10L);
            done.setStatus(SaleStatus.COMPLETED);
            return done;
        });

        assertThatThrownBy(() -> service.checkout(5L, ewalletThenCard()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be checked out");

        verifyNoInteractions(payments);
    }
}

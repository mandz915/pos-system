package com.pos.sales.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.pos.common.model.PaymentMethod;
import com.pos.sales.domain.CashMovementType;
import com.pos.sales.domain.Sale;
import com.pos.sales.domain.SaleLine;
import com.pos.sales.domain.SalePayment;
import com.pos.sales.domain.SaleReturn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SalesDtos {

    private SalesDtos() {
    }

    // ---- sales
    public record LineRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity) {
    }

    public record CreateSaleRequest(
            @NotBlank @Size(max = 64) String clientRef,
            @NotNull Long storeId,
            @NotNull Long terminalId,
            Long customerId,
            @NotEmpty @Valid List<LineRequest> lines) {
    }

    public record ReplaceLinesRequest(@NotEmpty @Valid List<LineRequest> lines) {
    }

    public record PaymentLine(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Size(max = 100) String reference) {
    }

    public record CheckoutRequest(@NotEmpty @Valid List<PaymentLine> payments) {
    }

    public record VoidRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record RefundLine(
            @NotNull Long saleLineId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity) {
    }

    /** {@code lines == null} refunds everything that has not been returned yet. */
    public record RefundRequest(
            @NotBlank @Size(max = 255) String reason,
            PaymentMethod method,
            Boolean restock,
            @Valid List<RefundLine> lines) {
    }

    public record SaleView(Sale sale, List<SaleLine> lines, List<SalePayment> payments, List<SaleReturn> returns) {
    }

    // ---- shifts
    public record OpenShiftRequest(
            @NotNull Long storeId,
            @NotNull Long terminalId,
            @NotNull @DecimalMin("0") BigDecimal openingCash) {
    }

    public record CloseShiftRequest(@NotNull @DecimalMin("0") BigDecimal countedCash) {
    }

    public record CashMovementRequest(
            @NotNull CashMovementType type,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(max = 255) String reason) {
    }

    /** X-read (while open) / Z-read (after closing). */
    public record ShiftReport(
            Long shiftId,
            Long storeId,
            Long terminalId,
            Long cashierId,
            String status,
            Instant openedAt,
            Instant closedAt,
            BigDecimal openingCash,
            long salesCount,
            BigDecimal grossSales,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal refundTotal,
            BigDecimal netSales,
            Map<String, BigDecimal> paymentsByMethod,
            BigDecimal cashIn,
            BigDecimal cashOut,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal variance) {
    }
}

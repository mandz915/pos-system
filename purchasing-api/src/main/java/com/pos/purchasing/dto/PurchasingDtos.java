package com.pos.purchasing.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PurchasingDtos {

    private PurchasingDtos() {
    }

    public record SupplierRequest(
            @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 150) String contact,
            @Size(max = 40) String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 255) String address) {
    }

    public record PoLineRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.001") BigDecimal orderedQty,
            @NotNull @DecimalMin("0") BigDecimal unitCost) {
    }

    public record CreatePoRequest(
            @NotNull Long supplierId,
            @NotNull Long storeId,
            @Size(max = 255) String note,
            @NotEmpty @Valid List<PoLineRequest> lines) {
    }

    public record UpdatePoRequest(
            @Size(max = 255) String note,
            @NotEmpty @Valid List<PoLineRequest> lines) {
    }

    public record ReceiveLine(
            @NotNull Long poLineId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity) {
    }

    public record ReceiveRequest(
            @Size(max = 255) String note,
            @NotEmpty @Valid List<ReceiveLine> lines) {
    }

    public record PoView(PurchaseOrder order, List<PoLine> lines, List<GoodsReceipt> receipts) {
    }
}

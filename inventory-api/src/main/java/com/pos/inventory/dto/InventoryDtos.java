package com.pos.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pos.inventory.domain.StockCount;
import com.pos.inventory.domain.StockCountLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record ReceiveRequest(
            @NotNull Long storeId,
            @NotNull Long productId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @Size(max = 255) String note) {
    }

    /** {@code delta} is signed: positive adds stock, negative removes it. */
    public record AdjustRequest(
            @NotNull Long storeId,
            @NotNull Long productId,
            @NotNull BigDecimal delta,
            @NotNull @Size(min = 1, max = 255) String reason) {
    }

    public record TransferRequest(
            @NotNull Long fromStoreId,
            @NotNull Long toStoreId,
            @NotNull Long productId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @Size(max = 255) String note) {
    }

    public record ReorderLevelRequest(
            @NotNull Long storeId,
            @NotNull Long productId,
            @NotNull @DecimalMin("0") BigDecimal reorderLevel) {
    }

    public record OpenCountRequest(@NotNull Long storeId) {
    }

    public record CountLineRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0") BigDecimal countedQty) {
    }

    public record CountLinesRequest(@NotEmpty @Valid List<CountLineRequest> lines) {
    }

    public record CountView(StockCount count, List<StockCountLine> lines) {
    }
}

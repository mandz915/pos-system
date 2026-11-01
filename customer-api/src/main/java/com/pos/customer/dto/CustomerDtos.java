package com.pos.customer.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 40) String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 255) String address,
            @DecimalMin("0") BigDecimal creditLimit) {
    }

    public record PointsRequest(
            @NotNull @Min(1) Long points,
            @Size(max = 255) String note) {
    }

    /** Signed manual correction of the points balance. */
    public record AdjustPointsRequest(
            @NotNull Long points,
            @NotBlank @Size(max = 255) String note) {
    }

    public record CreditRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Size(max = 20) String refType,
            @Size(max = 60) String refId,
            @Size(max = 255) String note) {
    }
}

package com.pos.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record StoreRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 255) String address,
            @Size(max = 40) String phone,
            @Size(max = 30) String tin) {
    }

    public record TerminalRequest(
            @NotNull Long storeId,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 100) String name) {
    }

    public record SettingRequest(
            Long storeId,
            @NotBlank @Size(max = 100) String key,
            @NotNull String value) {
    }
}

package com.pos.user.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TenantDtos {

    private TenantDtos() {
    }

    public record CreateTenantRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}", message = "Code must be 2 to 30 letters, digits, - or _")
            String code,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(min = 3, max = 60) String adminUsername,
            @NotBlank @Size(min = 8, max = 100) String adminPassword,
            @Size(max = 150) String adminFullName) {
    }

    public record UpdateTenantRequest(@NotBlank @Size(max = 150) String name) {
    }

    public record TenantView(Long id, String code, String name, String status, Instant createdAt) {
    }
}

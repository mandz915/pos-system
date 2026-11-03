package com.pos.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record LoginRequest(String tenant, @NotBlank String username, @NotBlank String password) {
    }

    public record PinLoginRequest(
            String tenant,
            @NotBlank String username,
            @NotBlank @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits") String pin) {
    }

    public record UserView(
            Long id,
            String username,
            String fullName,
            String email,
            Long storeId,
            boolean active,
            List<String> roles,
            Instant createdAt) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserView user) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 60) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Email String email,
            Long storeId,
            @NotEmpty Set<String> roles,
            @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits") String pin) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Email String email,
            Long storeId,
            @NotEmpty Set<String> roles) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    public record SetPinRequest(
            @NotBlank @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits") String pin) {
    }
}

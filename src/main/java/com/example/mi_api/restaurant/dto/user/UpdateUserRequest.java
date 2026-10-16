package com.example.mi_api.restaurant.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        @NotBlank
        String fullName,
        @NotBlank
        String username,
        String password,
        @NotBlank
        String role,
        @Pattern(regexp = "[0-9]{8}|")
        String dni,
        @Pattern(regexp = "[0-9]{9}|")
        String phone,
        Boolean active) {}

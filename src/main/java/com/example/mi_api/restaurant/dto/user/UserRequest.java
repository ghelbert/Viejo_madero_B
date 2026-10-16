package com.example.mi_api.restaurant.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
        @NotBlank
        String fullName,
        @NotBlank
        String username,
        @NotBlank
        String password,
        @NotBlank
        String role,
        @NotBlank
        @Pattern(regexp = "[0-9]{8}")
        String dni,
        @NotBlank
        @Pattern(regexp = "[0-9]{9}")
        String phone,
        Boolean active) {}

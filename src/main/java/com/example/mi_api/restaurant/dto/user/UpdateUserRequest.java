package com.example.mi_api.restaurant.dto.user;

public record UpdateUserRequest(
        String fullName,
        String username,
        String password,
        String role) {}

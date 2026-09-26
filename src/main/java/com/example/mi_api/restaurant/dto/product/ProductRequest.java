package com.example.mi_api.restaurant.dto.product;

import java.math.BigDecimal;

public record ProductRequest(
        String category,
        String name,
        String description,
        BigDecimal price,
        Integer prepMinutes,
        Boolean available) {}

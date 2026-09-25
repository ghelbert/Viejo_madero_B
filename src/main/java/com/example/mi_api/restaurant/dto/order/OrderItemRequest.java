package com.example.mi_api.restaurant.dto.order;

import java.math.BigDecimal;

public record OrderItemRequest(
        long productId,
        String productName,
        BigDecimal price,
        int quantity,
        String notes) {}

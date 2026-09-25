package com.example.mi_api.restaurant.dto.order;

import java.util.List;

public record CreateOrderRequest(
        long tableId,
        long createdBy,
        String customerName,
        List<OrderItemRequest> items) {}

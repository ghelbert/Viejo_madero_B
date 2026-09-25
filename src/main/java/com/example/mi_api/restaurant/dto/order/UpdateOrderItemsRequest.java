package com.example.mi_api.restaurant.dto.order;

import java.util.List;

public record UpdateOrderItemsRequest(
        String customerName,
        List<OrderItemRequest> items) {}

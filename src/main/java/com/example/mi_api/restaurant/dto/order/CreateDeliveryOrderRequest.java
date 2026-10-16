package com.example.mi_api.restaurant.dto.order;

import java.math.BigDecimal;
import java.util.List;

public record CreateDeliveryOrderRequest(
        String orderType,
        long createdBy,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String paymentMethod,
        String notes,
        List<OrderItemRequest> items) {}

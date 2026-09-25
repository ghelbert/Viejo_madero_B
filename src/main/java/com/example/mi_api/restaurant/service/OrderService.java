package com.example.mi_api.restaurant.service;

import com.example.mi_api.restaurant.dto.order.CreateOrderRequest;
import com.example.mi_api.restaurant.dto.order.StatusRequest;
import com.example.mi_api.restaurant.dto.order.UpdateOrderItemsRequest;
import com.example.mi_api.restaurant.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Map<String, Object> create(CreateOrderRequest request) {
        var code = "#" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var total = calculateTotal(request.items());

        var orderId = orderRepository.createOrder(
                code,
                request.tableId(),
                request.createdBy(),
                request.customerName(),
                total);

        orderRepository.insertItems(orderId, request.items());
        orderRepository.markTableOccupied(request.tableId());

        return Map.of("id", orderId, "code", code, "status", "OPEN", "total", total);
    }

    @Transactional
    public Map<String, Object> confirm(long id, long userId) {
        transition(id, "CONFIRMED", userId);
        return orderRepository.findSummary(id);
    }

    public List<Map<String, Object>> findAll(String status) {
        return orderRepository.findAll(status);
    }

    public Map<String, Object> findDetail(long id) {
        return orderRepository.findDetail(id);
    }

    @Transactional
    public Map<String, Object> updateStatus(long id, StatusRequest request) {
        transition(id, request.status(), request.userId());
        return orderRepository.findSummary(id);
    }

    @Transactional
    public Map<String, Object> updateItems(long id, UpdateOrderItemsRequest request) {
        var total = calculateTotal(request.items());
        orderRepository.updateHeader(id, request.customerName(), total);
        orderRepository.replaceItems(id, request.items());
        return orderRepository.findSummary(id);
    }

    @Transactional
    public void delete(long id) {
        var tableId = orderRepository.findTableId(id);
        orderRepository.delete(id);

        if (tableId != null) {
            orderRepository.markTableFree(tableId);
        }
    }

    private void transition(long id, String status, long userId) {
        var current = orderRepository.findStatus(id);
        var tableId = orderRepository.findTableId(id);

        orderRepository.updateStatus(id, status, userId);
        orderRepository.insertStatusHistory(id, current, status, userId);

        if ("SERVED".equals(status) && tableId != null) {
            orderRepository.markTableFree(tableId);
        }
    }

    private BigDecimal calculateTotal(
            List<com.example.mi_api.restaurant.dto.order.OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

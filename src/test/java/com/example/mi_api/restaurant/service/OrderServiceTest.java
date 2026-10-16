package com.example.mi_api.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.mi_api.restaurant.dto.order.StatusRequest;
import com.example.mi_api.restaurant.dto.order.CreateDeliveryOrderRequest;
import com.example.mi_api.restaurant.repository.OrderRepository;
import com.example.mi_api.restaurant.websocket.OrderUpdatesWebSocketHandler;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderUpdatesWebSocketHandler orderUpdates;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderUpdates = mock(OrderUpdatesWebSocketHandler.class);
        orderService = new OrderService(orderRepository, orderUpdates);
    }

    @Test
    void cannotSkipPreparationAndMarkANewOrderReady() {
        when(orderRepository.findStatus(12)).thenReturn("OPEN");
        when(orderRepository.findUserRole(7)).thenReturn("COCINERO");

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateStatus(12, new StatusRequest("READY", 7)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).updateStatus(anyLong(), eq("READY"), anyLong());
    }

    @Test
    void startsRouteOnlyForAReadyDeliveryAndRecordsTheAssignment() {
        when(orderRepository.findUserRole(9)).thenReturn("REPARTIDOR");
        when(orderRepository.findOrderType(22)).thenReturn("DELIVERY");
        when(orderRepository.findStatus(22)).thenReturn("READY");
        when(orderRepository.startDeliveryRoute(22, 9)).thenReturn(1);
        when(orderRepository.findSummary(22)).thenReturn(Map.of(
                "id", 22L, "code", "PED-001", "status", "IN_ROUTE", "total", 28));

        var result = orderService.startDeliveryRoute(22, 9);

        assertEquals("IN_ROUTE", result.get("status"));
        verify(orderRepository).insertStatusHistory(22, "READY", "IN_ROUTE", 9);
    }

    @Test
    void rejectsRouteStartWhenOrderIsNotReady() {
        when(orderRepository.findUserRole(9)).thenReturn("REPARTIDOR");
        when(orderRepository.findOrderType(22)).thenReturn("DELIVERY");
        when(orderRepository.findStatus(22)).thenReturn("IN_ROUTE");

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.startDeliveryRoute(22, 9));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).startDeliveryRoute(anyLong(), anyLong());
    }

    @Test
    void rejectsAnEmptyOrderBeforeCreatingIt() {
        var exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createDelivery(new CreateDeliveryOrderRequest(
                        "DELIVERY", 1, "", "", "", null, null, "YAPE", "", List.of())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).findUserRole(anyLong());
    }
}

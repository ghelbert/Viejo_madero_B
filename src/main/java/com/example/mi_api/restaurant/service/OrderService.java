package com.example.mi_api.restaurant.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.mi_api.restaurant.dto.order.CompleteDeliveryRequest;
import com.example.mi_api.restaurant.dto.order.CreateDeliveryOrderRequest;
import com.example.mi_api.restaurant.dto.order.CreateOrderRequest;
import com.example.mi_api.restaurant.dto.order.StatusRequest;
import com.example.mi_api.restaurant.dto.order.UpdateOrderItemsRequest;
import com.example.mi_api.restaurant.repository.OrderRepository;
import com.example.mi_api.restaurant.websocket.OrderUpdatesWebSocketHandler;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderUpdatesWebSocketHandler orderUpdates;

    public OrderService(
            OrderRepository orderRepository,
            OrderUpdatesWebSocketHandler orderUpdates) {
        this.orderRepository = orderRepository;
        this.orderUpdates = orderUpdates;
    }

    @Transactional
    public Map<String, Object> create(CreateOrderRequest request) {
        requireItems(request.items());
        requireText(request.customerName(), "Ingresa el nombre del cliente");
        var items = resolveItems(request.items());
        if (!"MOZO".equals(orderRepository.findUserRole(request.createdBy()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un mozo puede registrar pedidos de salón");
        }
        if (!"FREE".equals(orderRepository.findTableStatus(request.tableId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa seleccionada ya no está disponible");
        }
        var code = "#" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var total = calculateTotal(items);

        var orderId = orderRepository.createOrder(
                code,
                request.tableId(),
                request.createdBy(),
                request.customerName(),
                total);

        orderRepository.insertItems(orderId, items);
        if (orderRepository.markTableOccupied(request.tableId()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa seleccionada ya no está disponible");
        }
        orderUpdates.publishAfterCommit();

        return Map.of("id", orderId, "code", code, "status", "OPEN", "total", total);
    }

    @Transactional
    public Map<String, Object> createDelivery(CreateDeliveryOrderRequest request) {
        requireItems(request.items());
        var items = resolveItems(request.items());
        if (!"RECEPCIONISTA".equals(orderRepository.findUserRole(request.createdBy()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo una recepcionista puede registrar pedidos de recepción");
        }
        if (!"DELIVERY".equals(request.orderType()) && !"TAKEAWAY".equals(request.orderType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de pedido no válido");
        }
        requireText(request.customerName(), "Ingresa el nombre del cliente");
        requireText(request.customerPhone(), "Ingresa el teléfono del cliente");
        if (!List.of("YAPE", "PLIN", "CASH").contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un método de pago válido");
        }
        if ("DELIVERY".equals(request.orderType())) {
            requireText(request.deliveryAddress(), "La dirección de entrega es obligatoria");
            if (request.deliveryLatitude() == null || request.deliveryLongitude() == null
                    || request.deliveryLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                    || request.deliveryLatitude().compareTo(BigDecimal.valueOf(90)) > 0
                    || request.deliveryLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                    || request.deliveryLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Registra una ubicación válida para la entrega");
            }
        }

        var code = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var total = calculateTotal(items);
        var orderId = orderRepository.createDeliveryOrder(code, request, total);
        orderRepository.insertItems(orderId, items);
        orderUpdates.publishAfterCommit();
        return Map.of("id", orderId, "code", code, "status", "OPEN", "total", total);
    }

    @Transactional
    public Map<String, Object> confirm(long id, long userId) {
        transition(id, "CONFIRMED", userId);
        orderUpdates.publishAfterCommit();
        return orderRepository.findSummary(id);
    }

    public List<Map<String, Object>> findAll(String status, String orderType) {
        return orderRepository.findAll(status, orderType);
    }

    public List<Map<String, Object>> findDeliveryDispatch() {
        return orderRepository.findAll(null, "DELIVERY").stream()
                .filter(order -> List.of("READY", "IN_ROUTE").contains(order.get("status")))
                .toList();
    }

    public List<Map<String, Object>> findDeliveryHistory() {
        return orderRepository.findAll("DELIVERED", "DELIVERY");
    }

    public Map<String, Object> findDetail(long id) {
        return orderRepository.findDetail(id);
    }

    @Transactional
    public Map<String, Object> updateStatus(long id, StatusRequest request) {
        transition(id, request.status(), request.userId());
        orderUpdates.publishAfterCommit();
        return orderRepository.findSummary(id);
    }

    @Transactional
    public Map<String, Object> updateItems(long id, UpdateOrderItemsRequest request) {
        var status = orderRepository.findStatus(id);
        if (!List.of("OPEN", "CONFIRMED").contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El pedido ya fue enviado a cocina y no se puede editar");
        }
        requireItems(request.items());
        requireText(request.customerName(), "Ingresa el nombre del cliente");
        var items = resolveItems(request.items());
        var total = calculateTotal(items);
        orderRepository.updateHeader(id, request.customerName(), total);
        orderRepository.replaceItems(id, items);
        orderUpdates.publishAfterCommit();
        return orderRepository.findSummary(id);
    }

    @Transactional
    public void delete(long id) {
        var tableId = orderRepository.findTableId(id);
        orderRepository.delete(id);

        if (tableId != null) {
            orderRepository.markTableFree(tableId);
        }
        orderUpdates.publishAfterCommit();
    }

    @Transactional
    public Map<String, Object> startDeliveryRoute(long id, long userId) {
        requireDeliveryRole(userId);
        if (!"DELIVERY".equals(orderRepository.findOrderType(id))
                || !"READY".equals(orderRepository.findStatus(id))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El pedido no se encuentra disponible para iniciar ruta");
        }
        if (orderRepository.startDeliveryRoute(id, userId) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El pedido no se encuentra disponible para iniciar ruta");
        }
        orderRepository.insertStatusHistory(id, "READY", "IN_ROUTE", userId);
        orderUpdates.publishAfterCommit();
        return orderRepository.findSummary(id);
    }

    @Transactional
    public Map<String, Object> completeDelivery(long id, CompleteDeliveryRequest request) {
        requireDeliveryRole(request.userId());
        if (!"DELIVERY".equals(orderRepository.findOrderType(id))
                || !"IN_ROUTE".equals(orderRepository.findStatus(id))
                || !java.util.Objects.equals(orderRepository.findDeliveryUserId(id), request.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo el repartidor asignado puede confirmar esta entrega");
        }
        if (orderRepository.completeDelivery(id, request.userId(), request.note()) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo el repartidor asignado puede confirmar esta entrega");
        }
        orderRepository.insertStatusHistory(id, "IN_ROUTE", "DELIVERED", request.userId());
        orderUpdates.publishAfterCommit();
        return orderRepository.findSummary(id);
    }

    private void transition(long id, String status, long userId) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido no válido");
        }
        var current = orderRepository.findStatus(id);
        var tableId = orderRepository.findTableId(id);
        var requiredRole = switch (status) {
            case "CONFIRMED", "SERVED" -> "MOZO";
            case "IN_KITCHEN", "READY" -> "COCINERO";
            default -> null;
        };
        if (requiredRole != null && !requiredRole.equals(orderRepository.findUserRole(userId))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para cambiar el estado del pedido");
        }

        var allowed = switch (status) {
            case "CONFIRMED" -> "OPEN".equals(current);
            case "IN_KITCHEN" -> List.of("OPEN", "CONFIRMED").contains(current);
            case "READY" -> "IN_KITCHEN".equals(current);
            case "SERVED" -> "READY".equals(current)
                    && "DINE_IN".equals(orderRepository.findOrderType(id));
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede cambiar el estado del pedido de " + current + " a " + status);
        }
        orderRepository.updateStatus(id, status, userId);
        orderRepository.insertStatusHistory(id, current, status, userId);

        if ("SERVED".equals(status) && tableId != null) {
            orderRepository.markTableAttended(tableId);
        }
    }

    private void requireDeliveryRole(long userId) {
        if (!"REPARTIDOR".equals(orderRepository.findUserRole(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un repartidor puede gestionar entregas");
        }
    }

    private void requireItems(List<?> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El pedido debe incluir al menos un plato");
        }
    }

    private List<com.example.mi_api.restaurant.dto.order.OrderItemRequest> resolveItems(
            List<com.example.mi_api.restaurant.dto.order.OrderItemRequest> items) {
        return items.stream()
                .map(item -> {
                    if (item == null || item.quantity() <= 0) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Cada plato debe tener una cantidad mayor que cero");
                    }
                    var product = orderRepository.findProductForOrder(item.productId());
                    if (product == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Uno de los platos no está disponible");
                    }
                    return new com.example.mi_api.restaurant.dto.order.OrderItemRequest(
                            item.productId(),
                            (String) product.get("name"),
                            (BigDecimal) product.get("base_price"),
                            item.quantity(),
                            item.notes());
                })
                .toList();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private BigDecimal calculateTotal(
            List<com.example.mi_api.restaurant.dto.order.OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

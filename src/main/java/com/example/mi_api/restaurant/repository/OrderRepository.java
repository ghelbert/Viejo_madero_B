package com.example.mi_api.restaurant.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.mi_api.restaurant.dto.order.CreateDeliveryOrderRequest;
import com.example.mi_api.restaurant.dto.order.OrderItemRequest;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long createOrder(
            String code,
            long tableId,
            long createdBy,
            String customerName,
            BigDecimal total) {
        return jdbc.queryForObject(
                "INSERT INTO orders(code,table_id,created_by,customer_name,subtotal,total) "
                        + "VALUES (?,?,?,?,?,?) RETURNING id",
                Long.class,
                code,
                tableId,
                createdBy,
                customerName,
                total,
                total);
    }

    public Long createDeliveryOrder(
            String code,
            CreateDeliveryOrderRequest request,
            BigDecimal total) {
        return jdbc.queryForObject(
                "INSERT INTO orders(code,order_type,created_by,customer_name,customer_phone,"
                        + "delivery_address,delivery_latitude,delivery_longitude,payment_method,"
                        + "delivery_notes,subtotal,total) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
                Long.class,
                code,
                request.orderType(),
                request.createdBy(),
                request.customerName(),
                request.customerPhone(),
                request.deliveryAddress(),
                request.deliveryLatitude(),
                request.deliveryLongitude(),
                request.paymentMethod(),
                request.notes(),
                total,
                total);
    }

    public void insertItems(long orderId, List<OrderItemRequest> items) {
        for (var item : items) {
            jdbc.update(
                    "INSERT INTO order_items(order_id,product_id,product_name,unit_price,quantity,line_total,notes) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    orderId,
                    item.productId(),
                    item.productName(),
                    item.price(),
                    item.quantity(),
                    item.price().multiply(BigDecimal.valueOf(item.quantity())),
                    item.notes());
        }
    }

    public int markTableOccupied(long tableId) {
        return jdbc.update(
                "UPDATE restaurant_tables SET status='OCCUPIED' WHERE id=? AND status='FREE' AND active",
                tableId);
    }

    public void markTableAttended(long tableId) {
        jdbc.update("UPDATE restaurant_tables SET status='ATTENDED' WHERE id=?", tableId);
    }

    public List<Map<String, Object>> findAll(String status, String orderType) {
        var sql = "SELECT o.id,o.code,o.order_type,o.status,o.total,o.customer_name,"
                + "o.customer_phone,o.delivery_address,o.delivery_latitude,o.delivery_longitude,"
                + "o.payment_method,o.delivery_notes,o.created_at,o.ready_at,o.route_started_at,o.delivered_at,"
                + "o.delivery_user_id,(SELECT COUNT(*) FROM order_items oi WHERE oi.order_id=o.id) item_count,"
                + "t.code table_code,u.full_name waiter "
                + "FROM orders o LEFT JOIN restaurant_tables t ON t.id=o.table_id "
                + "JOIN users u ON u.id=o.created_by "
                + "WHERE o.status <> 'CANCELLED'";

        var parameters = new java.util.ArrayList<Object>();
        if (status != null) {
            sql += " AND o.status=?";
            parameters.add(status);
        }
        if (orderType != null) {
            sql += " AND o.order_type=?";
            parameters.add(orderType);
        }
        sql += " ORDER BY o.created_at";
        if (status == null && orderType == null) {
            sql += " DESC";
        } else {
            sql += " ASC";
        }
        return jdbc.queryForList(sql, parameters.toArray());
    }

    public Map<String, Object> findDetail(long id) {
        var order = jdbc.queryForMap(
                "SELECT o.id,o.code,o.order_type,o.status,o.total,o.customer_name,o.customer_phone,"
                        + "o.delivery_address,o.delivery_latitude,o.delivery_longitude,o.payment_method,"
                        + "o.delivery_notes,o.ready_at,o.route_started_at,o.delivered_at,o.delivery_completion_note,"
                        + "o.created_at,"
                        + "t.code table_code,u.full_name waiter "
                        + "FROM orders o LEFT JOIN restaurant_tables t ON t.id=o.table_id "
                        + "JOIN users u ON u.id=o.created_by WHERE o.id=?", id);
        order.put("items", findItems(id));
        return order;
    }

    public Map<String, Object> findSummary(long id) {
        return jdbc.queryForMap(
                "SELECT id,code,status,total,table_id FROM orders WHERE id=?", id);
    }

    public List<Map<String, Object>> findItems(long orderId) {
        return jdbc.queryForList(
                "SELECT product_id, product_name, unit_price, quantity, line_total "
                        + ", notes FROM order_items WHERE order_id=? ORDER BY id", orderId);
    }

    public String findStatus(long id) {
        var statuses = jdbc.queryForList(
                "SELECT status FROM orders WHERE id=?", String.class, id);
        return statuses.isEmpty() ? null : statuses.getFirst();
    }

    public String findOrderType(long id) {
        var types = jdbc.queryForList(
                "SELECT order_type FROM orders WHERE id=?", String.class, id);
        return types.isEmpty() ? null : types.getFirst();
    }

    public Long findDeliveryUserId(long id) {
        return jdbc.queryForObject(
                "SELECT delivery_user_id FROM orders WHERE id=?", Long.class, id);
    }

    public String findUserRole(long userId) {
        var roles = jdbc.queryForList(
                "SELECT r.name FROM users u JOIN roles r ON r.id=u.role_id "
                        + "WHERE u.id=? AND u.active",
                String.class,
                userId);
        return roles.isEmpty() ? null : roles.getFirst();
    }

    public String findTableStatus(long tableId) {
        var statuses = jdbc.queryForList(
                "SELECT status FROM restaurant_tables WHERE id=? AND active", String.class, tableId);
        return statuses.isEmpty() ? null : statuses.getFirst();
    }

    public Map<String, Object> findProductForOrder(long productId) {
        var products = jdbc.queryForList(
                "SELECT name,base_price FROM products WHERE id=? AND available", productId);
        return products.isEmpty() ? null : products.getFirst();
    }

    public Long findTableId(long id) {
        return jdbc.queryForObject(
                "SELECT table_id FROM orders WHERE id=?", Long.class, id);
    }

    public void updateStatus(long id, String status, long userId) {
        jdbc.update(
                "UPDATE orders SET status=?, "
                        + "confirmed_by=CASE WHEN ?='CONFIRMED' THEN ? ELSE confirmed_by END, "
                        + "confirmed_at=CASE WHEN ?='CONFIRMED' THEN NOW() ELSE confirmed_at END, "
                        + "ready_at=CASE WHEN ?='READY' THEN NOW() ELSE ready_at END, "
                        + "updated_at=NOW() WHERE id=?",
                status,
                status,
                userId,
                status,
                status,
                id);
    }

    public int startDeliveryRoute(long id, long userId) {
        return jdbc.update(
                "UPDATE orders SET status='IN_ROUTE',delivery_user_id=?,route_started_at=NOW(),"
                        + "updated_at=NOW() WHERE id=? AND order_type='DELIVERY' AND status='READY'",
                userId,
                id);
    }

    public int completeDelivery(long id, long userId, String note) {
        return jdbc.update(
                "UPDATE orders SET status='DELIVERED',delivered_at=NOW(),"
                        + "delivery_completion_note=?,updated_at=NOW() "
                        + "WHERE id=? AND order_type='DELIVERY' AND status='IN_ROUTE' "
                        + "AND delivery_user_id=?",
                note,
                id,
                userId);
    }

    public void insertStatusHistory(long orderId, String fromStatus, String toStatus, long userId) {
        jdbc.update(
                "INSERT INTO order_status_history(order_id,from_status,to_status,changed_by) "
                        + "VALUES (?,?,?,?)",
                orderId,
                fromStatus,
                toStatus,
                userId);
    }

    public void updateHeader(long id, String customerName, BigDecimal total) {
        jdbc.update(
                "UPDATE orders SET customer_name=?, subtotal=?, total=?, updated_at=NOW() WHERE id=?",
                customerName,
                total,
                total,
                id);
    }

    public void replaceItems(long orderId, List<OrderItemRequest> items) {
        jdbc.update("DELETE FROM order_items WHERE order_id=?", orderId);
        insertItems(orderId, items);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM orders WHERE id=?", id);
    }

    public void markTableFree(long tableId) {
        jdbc.update("UPDATE restaurant_tables SET status='FREE' WHERE id=?", tableId);
    }
}

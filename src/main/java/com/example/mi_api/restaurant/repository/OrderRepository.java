package com.example.mi_api.restaurant.repository;

import com.example.mi_api.restaurant.dto.order.OrderItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public void markTableOccupied(long tableId) {
        jdbc.update("UPDATE restaurant_tables SET status='OCCUPIED' WHERE id=?", tableId);
    }

    public List<Map<String, Object>> findAll(String status) {
        var sql = "SELECT o.id,o.code,o.status,o.total,o.customer_name,o.created_at,"
                + "t.code table_code,u.full_name waiter "
                + "FROM orders o LEFT JOIN restaurant_tables t ON t.id=o.table_id "
                + "JOIN users u ON u.id=o.created_by "
                + "WHERE o.status <> 'CANCELLED'";

        return status == null
                ? jdbc.queryForList(sql + " ORDER BY o.created_at DESC")
                : jdbc.queryForList(sql + " AND o.status=? ORDER BY o.created_at DESC", status);
    }

    public Map<String, Object> findDetail(long id) {
        var order = jdbc.queryForMap(
                "SELECT o.id,o.code,o.status,o.total,o.customer_name,o.created_at,"
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
                        + "FROM order_items WHERE order_id=? ORDER BY id", orderId);
    }

    public String findStatus(long id) {
        return jdbc.queryForObject(
                "SELECT status FROM orders WHERE id=?", String.class, id);
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
                        + "updated_at=NOW() WHERE id=?",
                status,
                status,
                userId,
                status,
                id);
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

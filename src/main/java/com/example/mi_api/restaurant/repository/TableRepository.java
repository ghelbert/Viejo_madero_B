package com.example.mi_api.restaurant.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TableRepository {

    private final JdbcTemplate jdbc;

    public TableRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT t.id, t.code, t.capacity, t.zone, t.status, t.active, "
                        + "current_order.customer_name "
                        + "FROM restaurant_tables t "
                        + "LEFT JOIN LATERAL (SELECT o.customer_name FROM orders o "
                        + "WHERE o.table_id=t.id AND o.status NOT IN ('SERVED','CANCELLED') "
                        + "ORDER BY o.created_at DESC LIMIT 1) current_order ON TRUE "
                        + "WHERE t.active ORDER BY t.id");
    }

    public Map<String, Object> findDetail(long id) {
        var table = jdbc.queryForMap(
                "SELECT id, code, capacity, zone, status, active "
                        + "FROM restaurant_tables WHERE id=?", id);
        var order = jdbc.queryForMap(
                "SELECT id, code, status, total, customer_name FROM orders "
                        + "WHERE table_id=? AND status <> 'CANCELLED' "
                        + "ORDER BY created_at DESC LIMIT 1", id);
        var items = jdbc.queryForList(
                "SELECT product_id, product_name, unit_price, quantity, line_total "
                        + "FROM order_items WHERE order_id=? ORDER BY id", order.get("id"));
        order.put("items", items);
        return Map.of("table", table, "order", order);
    }
}

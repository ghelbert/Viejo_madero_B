package com.example.mi_api.restaurant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RestaurantController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public RestaurantController(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }

@PostMapping("/auth/login")
Map<String, Object> login(@RequestBody LoginRequest request) {
    System.out.println(">>> LLEGÓ AL CONTROLLER: " + request.username());

    var users = jdbc.queryForList(
        "SELECT u.id, u.full_name, u.username, u.password_hash, r.name role " +
        "FROM users u JOIN roles r ON r.id=u.role_id " +
        "WHERE u.username=? AND u.active", request.username());

    System.out.println(">>> USUARIOS ENCONTRADOS: " + users.size());

    if (users.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
    }

    var user = users.getFirst();
    String hash = (String) user.get("password_hash");
    System.out.println(">>> HASH EN BD: " + hash);
    System.out.println(">>> PASSWORD RECIBIDO: " + request.password());
    System.out.println(">>> MATCHES: " + encoder.matches(request.password(), hash));

    if (!encoder.matches(request.password(), hash)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña incorrecta");
    }

    return Map.of(
        "id", user.get("id"),
        "fullName", user.get("full_name"),
        "username", user.get("username"),
        "role", user.get("role")
    );
}

    @GetMapping("/tables")
    List<Map<String, Object>> tables() { return jdbc.queryForList("SELECT id, code, capacity, zone, status, active FROM restaurant_tables WHERE active ORDER BY id"); }

    @GetMapping("/products")
    List<Map<String, Object>> products() { return jdbc.queryForList("SELECT p.id, p.name, p.description, p.base_price, p.available, c.name category FROM products p JOIN categories c ON c.id=p.category_id WHERE p.available ORDER BY c.sort_order, p.name"); }

    @PostMapping("/products")
    Map<String, Object> createProduct(@RequestBody ProductRequest request) {
        var id = jdbc.queryForObject("INSERT INTO products(category_id,name,description,base_price,prep_minutes) VALUES ((SELECT id FROM categories WHERE name=?),?,?,?,?) RETURNING id", Long.class, request.category(), request.name(), request.description(), request.price(), request.prepMinutes());
        return Map.of("id", id, "name", request.name());
    }

    @PatchMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateProduct(@PathVariable long id, @RequestBody ProductRequest request) { jdbc.update("UPDATE products SET name=?, description=?, base_price=?, available=?, updated_at=NOW() WHERE id=?", request.name(), request.description(), request.price(), request.available(), id); }

    @GetMapping("/users")
    List<Map<String, Object>> users() { return jdbc.queryForList("SELECT u.id, u.full_name, u.username, u.active, r.name role FROM users u JOIN roles r ON r.id=u.role_id ORDER BY u.full_name"); }

    @PostMapping("/users")
    Map<String, Object> createUser(@RequestBody UserRequest request) {
        var id = jdbc.queryForObject("INSERT INTO users(role_id,full_name,username,password_hash) VALUES ((SELECT id FROM roles WHERE name=?),?,?,?) RETURNING id", Long.class, request.role(), request.fullName(), request.username(), encoder.encode(request.password()));
        return Map.of("id", id, "username", request.username());
    }

    @PatchMapping("/users/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setUserActive(@PathVariable long id, @RequestParam boolean active) { jdbc.update("UPDATE users SET active=?, updated_at=NOW() WHERE id=?", active, id); }

    @PostMapping("/orders")
    Map<String, Object> createOrder(@RequestBody CreateOrderRequest request) {
        var code = "#" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var total = request.items().stream().map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        var orderId = jdbc.queryForObject("INSERT INTO orders(code,table_id,created_by,subtotal,total) VALUES (?,?,?,?,?) RETURNING id", Long.class, code, request.tableId(), request.createdBy(), total, total);
        for (var item : request.items()) jdbc.update("INSERT INTO order_items(order_id,product_id,product_name,unit_price,quantity,line_total,notes) VALUES (?,?,?,?,?,?,?)", orderId, item.productId(), item.productName(), item.price(), item.quantity(), item.price().multiply(BigDecimal.valueOf(item.quantity())), item.notes());
        jdbc.update("UPDATE restaurant_tables SET status='OCCUPIED' WHERE id=?", request.tableId());
        return Map.of("id", orderId, "code", code, "status", "OPEN", "total", total);
    }

    @PostMapping("/orders/{id}/confirm")
    Map<String, Object> confirmOrder(@PathVariable long id, @RequestParam long userId) { transition(id, "CONFIRMED", userId); return order(id); }

    @GetMapping("/orders")
    List<Map<String, Object>> orders(@RequestParam(required = false) String status) {
        var sql = "SELECT o.id,o.code,o.status,o.total,o.created_at,t.code table_code,u.full_name waiter FROM orders o LEFT JOIN restaurant_tables t ON t.id=o.table_id JOIN users u ON u.id=o.created_by WHERE o.status <> 'CANCELLED'";
        return status == null ? jdbc.queryForList(sql + " ORDER BY o.created_at DESC") : jdbc.queryForList(sql + " AND o.status=? ORDER BY o.created_at DESC", status);
    }

    @PostMapping("/orders/{id}/status")
    Map<String, Object> updateOrderStatus(@PathVariable long id, @RequestBody StatusRequest request) { transition(id, request.status(), request.userId()); return order(id); }

    private Map<String, Object> order(long id) { return jdbc.queryForMap("SELECT id,code,status,total,table_id FROM orders WHERE id=?", id); }
    private void transition(long id, String status, long userId) {
        var current = jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, id);
        jdbc.update("UPDATE orders SET status=?, confirmed_by=CASE WHEN ?='CONFIRMED' THEN ? ELSE confirmed_by END, confirmed_at=CASE WHEN ?='CONFIRMED' THEN NOW() ELSE confirmed_at END, updated_at=NOW() WHERE id=?", status, status, userId, status, id);
        jdbc.update("INSERT INTO order_status_history(order_id,from_status,to_status,changed_by) VALUES (?,?,?,?)", id, current, status, userId);
    }

    record LoginRequest(String username, String password) {}
    record ProductRequest(String category, String name, String description, BigDecimal price, Integer prepMinutes, Boolean available) {}
    record UserRequest(String fullName, String username, String password, String role) {}
    record OrderItemRequest(long productId, String productName, BigDecimal price, int quantity, String notes) {}
    record CreateOrderRequest(long tableId, long createdBy, List<OrderItemRequest> items) {}
    record StatusRequest(String status, long userId) {}
}

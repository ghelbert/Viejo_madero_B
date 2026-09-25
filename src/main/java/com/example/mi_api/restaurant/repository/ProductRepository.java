package com.example.mi_api.restaurant.repository;

import com.example.mi_api.restaurant.dto.product.ProductRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAvailable() {
        return jdbc.queryForList(
                "SELECT p.id, p.name, p.description, p.base_price, p.available, c.name category "
                        + "FROM products p JOIN categories c ON c.id=p.category_id "
                        + "WHERE p.available ORDER BY c.sort_order, p.name");
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT p.id, p.name, p.description, p.base_price, p.available, c.name category "
                        + "FROM products p JOIN categories c ON c.id=p.category_id "
                        + "ORDER BY c.sort_order, p.name");
    }

    public List<Map<String, Object>> findCategories() {
        return jdbc.queryForList(
                "SELECT id, name FROM categories WHERE active ORDER BY sort_order, name");
    }

    public Long create(ProductRequest request) {
        return jdbc.queryForObject(
                "INSERT INTO products(category_id,name,description,base_price,prep_minutes) "
                        + "VALUES ((SELECT id FROM categories WHERE name=?),?,?,?,?) RETURNING id",
                Long.class,
                request.category(),
                request.name(),
                request.description(),
                request.price(),
                request.prepMinutes());
    }

    public void update(long id, ProductRequest request) {
        jdbc.update(
                "UPDATE products SET name=?, description=?, base_price=?, available=?, "
                        + "updated_at=NOW() WHERE id=?",
                request.name(),
                request.description(),
                request.price(),
                request.available(),
                id);
    }
}

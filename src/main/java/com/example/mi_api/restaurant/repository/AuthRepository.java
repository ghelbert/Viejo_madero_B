package com.example.mi_api.restaurant.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByUsername(String username) {
        return jdbc.queryForList(
                "SELECT u.id, u.full_name, u.username, u.password_hash, r.name role "
                        + "FROM users u JOIN roles r ON r.id=u.role_id "
                        + "WHERE u.username=? AND u.active",
                username);
    }
}

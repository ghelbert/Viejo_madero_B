package com.example.mi_api.restaurant.repository;

import com.example.mi_api.restaurant.dto.user.UpdateUserRequest;
import com.example.mi_api.restaurant.dto.user.UserRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT u.id, u.full_name, u.username, u.active, r.name role "
                        + "FROM users u JOIN roles r ON r.id=u.role_id ORDER BY u.full_name");
    }

    public Long create(UserRequest request, String passwordHash) {
        return jdbc.queryForObject(
                "INSERT INTO users(role_id,full_name,username,password_hash) "
                        + "VALUES ((SELECT id FROM roles WHERE name=?),?,?,?) RETURNING id",
                Long.class,
                request.role(),
                request.fullName(),
                request.username(),
                passwordHash);
    }

    public void update(long id, UpdateUserRequest request) {
        jdbc.update(
                "UPDATE users SET role_id=(SELECT id FROM roles WHERE name=?), "
                        + "full_name=?, username=?, updated_at=NOW() WHERE id=?",
                request.role(),
                request.fullName(),
                request.username(),
                id);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update(
                "UPDATE users SET password_hash=?, updated_at=NOW() WHERE id=?",
                passwordHash,
                id);
    }

    public void setActive(long id, boolean active) {
        jdbc.update(
                "UPDATE users SET active=?, updated_at=NOW() WHERE id=?",
                active,
                id);
    }
}

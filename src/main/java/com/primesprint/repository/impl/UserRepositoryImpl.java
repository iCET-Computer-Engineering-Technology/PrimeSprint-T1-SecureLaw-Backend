package com.primesprint.repository.impl;

import com.primesprint.model.entity.Role;
import com.primesprint.model.entity.User;
import com.primesprint.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();

        user.setId(UUID.fromString(rs.getString("id")));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setStatus(rs.getString("status"));

        // Convert role string → enum
        String roleName = rs.getString("role_name");
        if (roleName != null) {
            user.setRole(com.primesprint.model.enums.Role.valueOf(roleName));
        }

        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));

        return user;
    };

    @Override
    public User findByUsernameOrEmail(String usernameOrEmail) {
        String sql = """
                SELECT u.id, u.username, u.email, u.password, u.status,u.created_at,
                       u.updated_at,
                       u.senior_id,
                       r.id AS role_id,
                       r.name AS role_name
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE u.username = ? OR u.email = ?
                """;

        List<User> results = jdbcTemplate.query(sql, (rs, rowNum) -> {

            Role role = new Role(
                    UUID.fromString(rs.getString("role_id")),
                    rs.getString("role_name")
            );

            User user = new User();
            user.setId(UUID.fromString(rs.getString("id")));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setStatus(rs.getString("status"));
            user.setCreatedAt(rs.getTimestamp("created_at"));
            user.setUpdatedAt(rs.getTimestamp("updated_at"));
            user.setSeniorId(
                    rs.getString("senior_id") != null ?
                            UUID.fromString(rs.getString("senior_id")) : null
            );

            user.setRole(com.primesprint.model.enums.Role.valueOf(role.getName()));

            return user;

        }, usernameOrEmail, usernameOrEmail);

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void saveUser(String username,
                         String email,
                         String password,
                         UUID roleId) {

        String sql = """
                INSERT INTO users (id, username, email, password, role_id, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                """;

        jdbcTemplate.update(sql, UUID.randomUUID(), username, email, password, roleId);
    }

    @Override
    public void updateUserStatus(UUID userId, String status) {

    }

    // =========================================================
    // 🔍 FIND BY ID
    // =========================================================
    @Override
    public User findById(UUID id) {

        String sql = """
            SELECT u.*, r.name AS role_name
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
        """;

        try {
            return jdbcTemplate.queryForObject(
                sql,userRowMapper,id
            );
        } catch (Exception ex) {
            return null; // same behavior as your existing repo
        }
    }
    // =========================================================
    // 🔒 UPDATE PASSWORD_CHANGED_AT
    // =========================================================
    @Override
    public void updatePasswordChangedAt(UUID userId) {

        String sql = """
            UPDATE users
            SET password_changed_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        jdbcTemplate.update(sql, userId);
    }
}

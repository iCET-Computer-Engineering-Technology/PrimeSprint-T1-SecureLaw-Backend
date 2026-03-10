package com.primesprint.repository.impl;

import com.primesprint.model.entity.User;
import com.primesprint.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users (id, username, email, password, role_id, status, senior_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRoleId(),
                user.getStatus(),
                user.getSeniorId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        return user;
    }

    @Override
    public boolean existsBySeniorId(UUID seniorId) {
        String sql = "SELECT COUNT(*) FROM users WHERE senior_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seniorId);
        return count != null && count > 0;
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = """
                SELECT u.id, u.username, u.email, u.password, u.status,
                       u.senior_id,
                       r.id AS role_id,
                       r.name AS role_name
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE u.id = ?
                """;

        return jdbcTemplate.query(sql, (rs) -> {
            if (rs.next()) {
                User user = new User();
                user.setId(UUID.fromString(rs.getString("id")));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setStatus(rs.getString("status"));
                user.setSeniorId(
                        rs.getString("senior_id") != null ?
                                UUID.fromString(rs.getString("senior_id")) : null
                );
                user.setRole(com.primesprint.model.enums.Role.valueOf(rs.getString("role_name")));
                return Optional.of(user);
            }
            return Optional.empty();
        }, id);
    }

    @Override
    public User update(User user) {
        return null;
    }
}

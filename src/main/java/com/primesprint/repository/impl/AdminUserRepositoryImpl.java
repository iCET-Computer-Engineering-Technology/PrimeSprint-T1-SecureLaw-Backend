package com.primesprint.repository.impl;

import com.primesprint.model.entity.User;
import com.primesprint.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}

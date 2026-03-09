package com.primesprint.repository.impl;

import com.primesprint.model.Role;
import com.primesprint.model.User;
import com.primesprint.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public User findByUsernameOrEmail(String usernameOrEmail) {

        String sql = """
            SELECT u.id, u.username, u.email, u.password, u.status,
                   u.senior_id,
                   r.id AS role_id,
                   r.name AS role_name
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE u.username = ? OR u.email = ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {

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
            user.setSeniorId(
                    rs.getString("senior_id") != null ?
                            UUID.fromString(rs.getString("senior_id")) : null
            );

            user.setRole(com.primesprint.model.enums.Role.valueOf(role.getName()));

            return user;

        }, usernameOrEmail, usernameOrEmail);
    }

    @Override
    public void saveUser(String username,
                         String email,
                         String password,
                         UUID roleId) {

        String sql = """
            INSERT INTO users (username,email,password,role_id)VALUES (?,?,?,(SELECT id FROM roles WHERE name = ?))
    """;

        jdbcTemplate.update(
                sql,
                username,
                email,
                password,
                roleId
        );
    }

    @Override
    public void updateUserStatus(UUID userId, String status) {

    }
}

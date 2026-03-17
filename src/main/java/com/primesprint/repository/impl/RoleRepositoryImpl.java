package com.primesprint.repository.impl;

import com.primesprint.model.entity.Role;
import com.primesprint.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UUID findRoleIdByName(String roleName) {
        String sql = "SELECT id FROM roles WHERE name = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> UUID.fromString(rs.getString("id")), roleName);
    }

    @Override
    public Optional<Role> findByName(String name) {
        String sql = "SELECT id, name FROM roles WHERE name = ?";
        return jdbcTemplate.query(sql, (rs) -> {
            if (rs.next()) {
                Role role = new Role(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name")
                );
                return Optional.of(role);
            }
            return Optional.empty();
        }, name);
    }
}

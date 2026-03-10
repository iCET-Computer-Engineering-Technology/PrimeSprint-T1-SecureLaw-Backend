package com.primesprint.repository.impl;

import com.primesprint.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}

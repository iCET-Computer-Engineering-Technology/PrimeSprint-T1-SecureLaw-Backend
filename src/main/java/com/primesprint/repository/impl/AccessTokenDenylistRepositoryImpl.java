package com.primesprint.repository.impl;

import com.primesprint.repository.AccessTokenDenylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccessTokenDenylistRepositoryImpl implements AccessTokenDenylistRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(String jti, UUID userId, Timestamp expiresAt) {
        jdbcTemplate.update("""
            INSERT INTO access_token_denylist (jti, user_id, expires_at)
            VALUES (?, ?, ?)
        """, jti, userId, expiresAt);
    }

    @Override
    public boolean existsByJti(String jti) {
        Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM access_token_denylist WHERE jti = ?
                """, Integer.class, jti);
        return count != null && count > 0;
    }
}

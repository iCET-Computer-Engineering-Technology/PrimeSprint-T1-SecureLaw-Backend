package com.primesprint.repository.impl;

import com.primesprint.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(UUID userId, String tokenHash, Timestamp expiresAt) {
        jdbcTemplate.update("""
            INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
            VALUES (?, ?, ?)
        """, userId, tokenHash, expiresAt);
    }

    @Override
    public void revoke(UUID id) {
        jdbcTemplate.update("""
            UPDATE refresh_tokens SET revoked = true WHERE id = ?""", id);
    }

    @Override
    public void replace(UUID oldTokenId, UUID newTokenId) {
        jdbcTemplate.update("""
            UPDATE refresh_tokens SET revoked = true, replaced_by_token_id = ? WHERE id = ?
           """
        , newTokenId, oldTokenId);
    }

    @Override
    public void revokeAllByUserId(UUID userId) {

    }

    @Override
    public Map<String, Object> findByTokenHash(String tokenHash) {
        return Map.of();
    }
}

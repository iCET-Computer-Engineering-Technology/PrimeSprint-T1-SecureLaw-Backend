package com.primesprint.repository.impl;

import com.primesprint.repository.PasswordResetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepositoryImpl implements PasswordResetRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void savePasswordResetToken(UUID userId, String token) {
        String sql = " INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES ( ?, ?, NOW() + INTERVAL '15 minutes')";
        jdbcTemplate.update(sql, userId, token);
    }

    @Override
    public boolean isAvailable(String reqId) {
        String sql = "SELECT COUNT(1) FROM password_reset_tokens WHERE token = ? AND expires_at > NOW()";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, reqId);
        return count != null && count > 0;
    }

    @Override
    public boolean resetPassword(String reqId, String password) {
        String sql = "UPDATE users u SET password = ? " +
                "FROM password_reset_tokens prt " +
                "WHERE prt.token = ? AND prt.user_id = u.id AND prt.expires_at > NOW()";
        int updated = jdbcTemplate.update(sql, password, reqId);

        if (updated > 0) {
            jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE token = ?", reqId);
            return true;
        }

        return false;
    }


}

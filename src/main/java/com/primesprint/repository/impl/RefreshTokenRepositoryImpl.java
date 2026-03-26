package com.primesprint.repository.impl;

import com.primesprint.repository.RefreshTokenRepository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    @Override
    public void save(UUID userId, String tokenHash, Timestamp expiresAt) {

    }

    @Override
    public void revoke(UUID id) {

    }

    @Override
    public void replace(UUID oldTokenId, UUID newTokenId) {

    }

    @Override
    public void revokeAllByUserId(UUID userId) {

    }

    @Override
    public Map<String, Object> findByTokenHash(String tokenHash) {
        return Map.of();
    }
}

package com.primesprint.repository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(UUID userId, String tokenHash, Timestamp expiresAt);

    void revoke(UUID id);

    void replace(UUID oldTokenId, UUID newTokenId);

    void revokeAllByUserId(UUID userId);

    Map<String,Object> findByTokenHash(String tokenHash);

}

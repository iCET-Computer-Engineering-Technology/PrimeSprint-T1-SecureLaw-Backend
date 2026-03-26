package com.primesprint.repository;

import java.sql.Timestamp;
import java.util.UUID;

public interface AccessTokenDenylistRepository {

    void save(String jti, UUID userId, Timestamp expiresAt);

    boolean existsByJti(String jti);
}

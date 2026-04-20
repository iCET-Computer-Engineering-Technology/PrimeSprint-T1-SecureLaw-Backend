package com.primesprint.repository;

import java.util.UUID;

public interface PasswordResetRepository {
    void savePasswordResetToken(UUID userId, String token);

    boolean isAvailable(String reqId);

    boolean resetPassword(String reqId, String password);
}

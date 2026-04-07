package com.primesprint.repository;

import com.primesprint.model.entity.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {
    UUID createProfile(UUID userId, String displayName);
    Optional<Profile> getProfile(UUID userId);
    Optional<UUID> findProfileIdByUserId(UUID userId);
}

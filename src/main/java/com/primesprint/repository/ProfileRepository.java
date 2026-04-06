package com.primesprint.repository;

import com.primesprint.model.entity.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {
    UUID createProfile(UUID userID, String displayName);
    Optional<Profile> getProfile(UUID userID);
    Optional<UUID> findProfileIdByUserId(UUID userId);
}

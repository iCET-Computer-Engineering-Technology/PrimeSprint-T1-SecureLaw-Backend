package com.primesprint.repository;

import com.primesprint.model.entity.Profile;

import java.util.UUID;

public interface ProfileRepository {
    UUID createProfile(UUID userID, String displayName);
    Profile getProfile(UUID userID);
}

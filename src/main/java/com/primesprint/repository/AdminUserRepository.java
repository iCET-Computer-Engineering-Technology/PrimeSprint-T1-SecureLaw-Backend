package com.primesprint.repository;

import com.primesprint.model.entity.User;

import java.util.UUID;

public interface AdminUserRepository {
    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    User save(User user);

    boolean existsBySeniorId(UUID seniorId);
}

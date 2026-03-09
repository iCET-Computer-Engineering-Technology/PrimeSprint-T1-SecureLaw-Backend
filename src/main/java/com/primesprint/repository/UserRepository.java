package com.primesprint.repository;

import com.primesprint.model.User;

import java.util.UUID;

public interface UserRepository {

    User findByUsernameOrEmail(String value);

    void saveUser(String username, String email, String password, UUID roleId);

    void updateUserStatus(UUID userId, String status);
}

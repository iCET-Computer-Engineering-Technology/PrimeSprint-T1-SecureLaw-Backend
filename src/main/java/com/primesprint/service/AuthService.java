package com.primesprint.service;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.dto.response.LoginResponse;
import com.primesprint.model.entity.User;

import java.util.UUID;

public interface AuthService {

    User authenticate(LoginRequest request);

    void register(RegisterRequest request);

    User getById(UUID id);

    // =========================================================
    // 👤 GET USER FROM TOKEN (USED IN /me)
    // =========================================================
    UserDto getUserFromToken(String token);

    UserDto getUserByUsername(String username);

    void updatePasswordChangedAt(UUID userId);

    boolean validateToken(String token);
}

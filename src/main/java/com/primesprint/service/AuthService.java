package com.primesprint.service;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.dto.response.LoginResponse;
import com.primesprint.model.entity.User;

import java.util.UUID;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    // =========================================================
    // 🔐 AUTHENTICATE (LOGIN VALIDATION ONLY)
    // =========================================================
    User authenticate(LoginRequest request);

    void register(RegisterRequest request);

    String loginAndGetToken(LoginRequest request);

    // =========================================================
    // 👤 GET USER BY ID (FOR REFRESH FLOW)
    // =========================================================
    User getById(UUID id);

    // =========================================================
    // 🔒 PASSWORD CHANGE SUPPORT
    // =========================================================
    void updatePasswordChangedAt(UUID userId);

    boolean validateToken(String token);

    UserDto getUserFromToken(String token);

    UserDto getUserByUsername(String username);
}

package com.primesprint.service;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    String loginAndGetToken(LoginRequest request);

    boolean validateToken(String token);

    UserDto getUserFromToken(String token);

    UserDto getUserByUsername(String username);
}

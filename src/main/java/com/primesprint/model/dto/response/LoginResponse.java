package com.primesprint.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Instant expiresAt;
    private String role;
}
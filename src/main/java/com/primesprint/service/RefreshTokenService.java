package com.primesprint.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

public interface RefreshTokenService {

    String create(UUID userId);

    Map<String, Object> verify(String refreshToken);

    void revoke(String refreshToken);
}

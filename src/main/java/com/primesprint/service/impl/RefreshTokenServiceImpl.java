package com.primesprint.service.impl;

import com.primesprint.config.JwtProperties;
import com.primesprint.repository.RefreshTokenRepository;
import com.primesprint.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public String create(UUID userId) {
        String token = generateToken();
        String tokenHash = sha256(token);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() + jwtProperties.getRefresh().getToken().getTtl());
        refreshTokenRepository.save(userId, tokenHash, timestamp);
        return token;
    }

    @Override
    public Map<String, Object> verify(String refreshToken) {
        String tokenHash = sha256(refreshToken);
        var record = refreshTokenRepository.findByTokenHash(tokenHash);
        return record;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

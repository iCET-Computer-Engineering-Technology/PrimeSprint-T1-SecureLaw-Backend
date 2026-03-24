package com.primesprint.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;
    private long expirationTime;
    private long refreshTokenExpirationTime;

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("JWT secret key must not be null or empty");
        }
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("JWT expiration time must be greater than zero");
        }
        if (refreshTokenExpirationTime <= 0) {
            throw new IllegalArgumentException("JWT refresh token expiration time must be greater than zero");
        }
    }
}

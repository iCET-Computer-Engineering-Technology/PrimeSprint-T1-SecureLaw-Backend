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
    private String secret;
    private Access access;
    private Refresh refresh;

    @Getter @Setter
    public static class Access{
        private Token token;
    }
    @Getter @Setter
    public static class Refresh{
        private Token token;
    }
    @Getter @Setter
    public static class Token{
        private long ttl;
    }

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("JWT secret key must not be null or empty");
        }
        if (access == null || access.getToken() == null) {
             throw new IllegalArgumentException("JWT access token configuration must not be null");
        }
        if (refresh == null || refresh.getToken() == null) {
             throw new IllegalArgumentException("JWT refresh token configuration must not be null");
        }
    }
}

package com.primesprint.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AccessTokenDenylist {
    private String jti;
    private UUID userId;
    private Instant expiresAt;

}

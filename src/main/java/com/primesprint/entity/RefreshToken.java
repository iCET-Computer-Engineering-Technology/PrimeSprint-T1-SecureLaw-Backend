package com.primesprint.entity;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private Boolean revoked;
    private Instant lastUsedAt;
    private UUID replacedByTokenId;
}

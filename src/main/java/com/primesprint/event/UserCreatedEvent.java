package com.primesprint.event;

public record UserCreatedEvent(
        Long userId,
        String email,
        String username,
        String accessLink,
        String temporaryPassword,
        String createdAt
) {}

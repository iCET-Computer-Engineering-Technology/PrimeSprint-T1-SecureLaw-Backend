package com.primesprint.event;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String username,
        String accessLink,
        String createdAt
) {}


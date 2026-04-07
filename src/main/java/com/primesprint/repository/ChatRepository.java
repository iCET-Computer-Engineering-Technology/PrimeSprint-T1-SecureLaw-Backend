package com.primesprint.repository;

import com.primesprint.model.entity.Chat;

import java.util.Optional;
import java.util.UUID;

public interface ChatRepository {
    UUID createChat(UUID profileId);
    Optional<Chat> findChat(UUID chatId);
    boolean existsByIdAndProfileId(UUID chatId, UUID profileId);
}

package com.primesprint.service.impl;

import com.primesprint.repository.*;
import com.primesprint.service.ChatService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    private final ChatTurnRepository chatTurnRepository;

    private final AdminUserRepository adminUserRepository;

    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public UUID createChat(String username) {

        UUID userId =
                adminUserRepository.findById(UUID.fromString(username))
                        .orElseThrow(() ->
                                new RuntimeException("User not found"))
                        .getId();

        UUID profileId =
                profileRepository.getProfile(userId)
                        .orElseThrow(() ->
                                new RuntimeException("Profile not found"))
                        .getId();

        return chatRepository.createChat(profileId);
    }

    @Override
    public UUID saveTurn(String username, UUID chatId, String userPrompt, String aiResponse, String modelName, Long latencyMs) {
        return null;
    }
}

package com.primesprint.service.impl;

import com.primesprint.model.entity.ChatTurn;
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
                adminUserRepository.findIdByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        UUID profileId =
                profileRepository.findProfileIdByUserId(userId)
                        .orElseThrow(() ->
                                new RuntimeException("Profile not found"));

        return chatRepository.createChat(profileId);
    }

    @Override
    @Transactional
    public UUID saveTurn(String username, UUID chatId, String userPrompt, String aiResponse, String modelName, Long latencyMs) {

        UUID userId =
                adminUserRepository.findIdByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        UUID profileId =
                profileRepository.findProfileIdByUserId(userId)
                        .orElseThrow(() ->
                                new RuntimeException("Profile not found"));

        boolean ownsChat =
                chatRepository
                        .existsByIdAndProfileId(
                                chatId,
                                profileId
                        );

        if (!ownsChat) {
            throw new RuntimeException(
                    "Unauthorized chat access"
            );
        }

        ChatTurn chatTurn = new ChatTurn();

        chatTurn.setChatId(chatId);
        chatTurn.setUserPrompt(userPrompt);
        chatTurn.setAiResponse(aiResponse);
        chatTurn.setModelName(modelName);
        chatTurn.setLatencyMs(latencyMs);

        return chatTurnRepository.save(chatTurn);
    }
}

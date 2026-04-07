package com.primesprint.service;

import java.util.UUID;

public interface ChatService {
    UUID createChat(String username);

    UUID saveTurn(String username,
                  UUID chatId,
                  String userPrompt,
                  String aiResponse,
                  String modelName,
                  Long latencyMs
    );
}

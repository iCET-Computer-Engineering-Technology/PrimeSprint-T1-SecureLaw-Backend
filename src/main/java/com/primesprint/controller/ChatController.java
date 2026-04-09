package com.primesprint.controller;

import com.primesprint.model.dto.request.CreateTurnRequest;
import com.primesprint.model.dto.response.CreateChatResponse;
import com.primesprint.model.dto.response.CreateTurnResponse;
import com.primesprint.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CreateChatResponse> createChat(@AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        UUID chatId = chatService.createChat(username);
        return ResponseEntity.ok(new CreateChatResponse(chatId));
    }

    @PostMapping("/{chatId}/turns")
    public ResponseEntity<CreateTurnResponse> saveTurn(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID chatId, @RequestBody CreateTurnRequest createTurnRequest){
        String username = userDetails.getUsername();

        UUID turnId = chatService.saveTurn(
                username,
                chatId,
                createTurnRequest.getUserPrompt(),
                createTurnRequest.getAiResponse(),
                createTurnRequest.getModelName(),
                createTurnRequest.getLatencyMs()
        );

        return ResponseEntity.ok(
                new CreateTurnResponse(turnId));
    }
}

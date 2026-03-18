package com.primesprint.controller;

import com.primesprint.model.dto.ChatMessageRequest;
import com.primesprint.model.dto.ChatMessageResponse;
import com.primesprint.model.dto.SessionResponse;
import com.primesprint.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@CrossOrigin(origins="http://localhost:4200")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversation")
    public ResponseEntity<SessionResponse> newConversation() {
        SessionResponse session = chatService.createNewConversation();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody ChatMessageRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ChatMessageResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }
}
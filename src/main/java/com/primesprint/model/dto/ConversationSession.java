package com.primesprint.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConversationSession {

    private String conversationId;
    private LocalDateTime createdAt;
    private List<Message> messages = new ArrayList<>();

    public ConversationSession(String conversationId) {
        this.conversationId = conversationId;
        this.createdAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    // ── Inner Message class ─────────────────────────────────────

    public static class Message {
        private String role;
        private String content;
        private LocalDateTime timestamp;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now();
        }

        public String getRole()             { return role; }
        public String getContent()          { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}

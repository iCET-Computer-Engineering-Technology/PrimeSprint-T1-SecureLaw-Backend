package com.primesprint.service;

import com.primesprint.model.dto.ChatMessageRequest;
import com.primesprint.model.dto.ChatMessageResponse;
import com.primesprint.model.dto.ConversationSession;
import com.primesprint.model.dto.SessionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    // In-memory store: conversationId → session
    private final Map<String, ConversationSession> store = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    // 1. Create new conversation session
    // ─────────────────────────────────────────────────────────────
    public SessionResponse createNewConversation() {
        String conversationId = "conv_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 7);

        ConversationSession session = new ConversationSession(conversationId);
        store.put(conversationId, session);

        return new SessionResponse(conversationId, session.getCreatedAt());
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Send message and get mock AI response
    // ─────────────────────────────────────────────────────────────
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        String conversationId = request.getConversationId();
        String userText = request.getMessage();

        // Auto-create session if not found
        store.computeIfAbsent(conversationId, ConversationSession::new);
        ConversationSession session = store.get(conversationId);

        // Save user message
        session.addMessage(new ConversationSession.Message("user", userText));

        // Generate mock AI reply
        String aiReply = generateMockAiReply(userText);

        // Save AI message
        session.addMessage(new ConversationSession.Message("ai", aiReply));

        return new ChatMessageResponse(conversationId, userText, aiReply, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────
    // Mock AI — keyword-based responses
    // ─────────────────────────────────────────────────────────────
    private String generateMockAiReply(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("nda") || lower.contains("non-disclosure")) {
            return "I've reviewed the NDA. Clause 8.2 has unlimited indemnification with no monetary ceiling, " +
                    "and Clause 11.4 compounds breach penalties quarterly. Recommend inserting a hard liability cap under Schedule A.";
        }
        if (lower.contains("breach") || lower.contains("penalty") || lower.contains("damages")) {
            return "The liquidated damages clause may be challenged as a penalty under contract law. " +
                    "Reformulate it as a schedule tied to documented loss categories with a 30-day cure period.";
        }
        if (lower.contains("arbitration") || lower.contains("dispute")) {
            return "The arbitration clause grants unilateral appointment rights, which is void under the 2015 Amendment. " +
                    "A three-member SIAC panel is recommended.";
        }
        if (lower.contains("contract") || lower.contains("agreement") || lower.contains("clause")) {
            return "The clause appears enforceable under the Indian Contract Act §23. " +
                    "However, liability exposure is asymmetric. Recommend mutual indemnification cap and a force majeure carve-out.";
        }
        if (lower.contains("employ") || lower.contains("termination")) {
            return "Employment termination must comply with the Industrial Disputes Act, 1947. " +
                    "A notice period under 30 days for tenured employees may be unenforceable. Shall I draft a compliant notice?";
        }
        if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello! How can I assist you with your legal query today?";
        }
        if (lower.contains("bye") || lower.contains("goodbye")) {
            return "Goodbye! Feel free to return if you need further assistance.";
        }

        return "Thank you for your query. Could you clarify the applicable jurisdiction and the specific " +
                "clause or obligation in question? This will help me provide precise advice.";
    }
}
package com.primesprint.repository.impl;

import com.primesprint.model.entity.ChatTurn;
import com.primesprint.repository.ChatTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatTurnRepositoryImpl implements ChatTurnRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UUID save(ChatTurn chat) {

        UUID turnId = UUID.randomUUID();

        String sql = """
                INSERT INTO chat_turns
                (
                    id,
                    chat_id,
                    user_prompt,
                    ai_response,
                    model_name,
                    latency_ms,
                    created_at
                )
                VALUES(?,?,?,?,?,?,now())
                """;

        jdbcTemplate.update(
                sql,
                turnId,
                chat.getChatId(),
                chat.getUserPrompt(),
                chat.getAiResponse(),
                chat.getModelName(),
                chat.getLatencyMs()
        );

        return turnId;
    }
}

package com.primesprint.mapper;

import com.primesprint.model.entity.ChatTurn;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChatTurnRowMapper implements RowMapper<ChatTurn> {

    @Override
    public ChatTurn mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ChatTurn.builder()
                .id(UUID.fromString(rs.getString("id")))
                .chatId(UUID.fromString(rs.getString("chat_id")))
                .userPrompt(rs.getString("user_prompt"))
                .aiResponse(rs.getString("ai_response"))
                .modelName(rs.getString("model_name"))
                .latencyMs(rs.getLong("latency_ms"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}

package com.primesprint.repository.impl;

import com.primesprint.mapper.ChatRowMapper;
import com.primesprint.model.entity.Chat;
import com.primesprint.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UUID createChat(UUID profileId) {

        UUID chatId = UUID.randomUUID();

        String sql = """
                INSERT INTO chats
                (
                    id,
                    profile_id,
                    status,
                    created_at,
                    updated_at
                )
                VALUES(?,?,?,now(),now())
                """;

        jdbcTemplate.update(sql, chatId, profileId, "ACTIVE");
        return chatId;
    }

    @Override
    public Optional<Chat> findChat(UUID chatId) {
        String sql = """
                SELECT *
                FROM chats
                WHERE id = ?
                """;
        return jdbcTemplate.query(
                sql,
                new ChatRowMapper(),
                chatId
        ).stream().findFirst();
    }

    @Override
    public boolean existsByIdAndProfileId(UUID chatId, UUID profileId) {
        String sql = """
                SELECT COUNT(1) FROM chats
                WHERE id = ?
                AND profile_id = ?
                """;

        Integer count =
                jdbcTemplate.queryForObject(
                        sql,
                        Integer.class,
                        chatId,
                        profileId
                );

        return count != null && count > 0;
    }
}

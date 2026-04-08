package com.primesprint.mapper;


import com.primesprint.model.entity.Chat;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChatRowMapper implements RowMapper<Chat> {

    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Chat.builder()
                .id(UUID.fromString(rs.getString("id")))
                .profileId(UUID.fromString(rs.getString("profile_id")))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}

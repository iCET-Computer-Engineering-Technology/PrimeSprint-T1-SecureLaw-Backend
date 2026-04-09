package com.primesprint.mapper;

import com.primesprint.model.entity.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

@Component
public class ProfileRowMapper implements RowMapper<Profile> {

    @Override
    public Profile mapRow(ResultSet rs, int rowNum)
            throws SQLException {

        return Profile.builder()
                .id(UUID.fromString(rs.getString("id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .displayName(rs.getString("display_name"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}

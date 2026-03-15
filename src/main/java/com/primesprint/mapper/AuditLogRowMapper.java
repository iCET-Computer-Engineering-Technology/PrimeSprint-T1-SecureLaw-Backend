package com.primesprint.mapper;

import com.primesprint.enums.ActionType;
import com.primesprint.model.AuditLog;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuditLogRowMapper implements RowMapper<AuditLog> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AuditLog mapRow(ResultSet rs, int rowNum) throws SQLException {

        try {

            String json = rs.getString("mask_counts");

            Map<String,Integer> maskCounts = new HashMap<>();

            if (json != null) {
                maskCounts = mapper.readValue(
                        json,
                        new TypeReference<Map<String,Integer>>() {}
                );
            }
            return new AuditLog(
                    rs.getString("id"),
                    rs.getString("user_id"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getString("target"),
                    ActionType.valueOf(rs.getString("action")),
                    rs.getString("template_id"),
                    maskCounts,
                    rs.getString("model_used"),
                    rs.getLong("response_time"),
                    rs.getString("details")
            );

        } catch (Exception e) {
            throw new SQLException(e);
        }
    }


}
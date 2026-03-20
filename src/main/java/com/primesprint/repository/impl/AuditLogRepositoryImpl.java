package com.primesprint.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.enums.ActionType;
import com.primesprint.mapper.AuditLogRowMapper;
import com.primesprint.model.AuditLog;
import com.primesprint.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void insertAuditLog(AuditLog log) throws SQLException, JsonProcessingException {

        String sql = """
            INSERT INTO audit_log
            (id,user_id, timestamp, target,action, template_id,mask_counts, model_used, response_time, details)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?,?,?)""";


        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(objectMapper.writeValueAsString(log.getMaskCounts()));

        jdbcTemplate.update(
                sql,
                log.getId(),
                log.getUserId(),
                log.getTimestamp(),
                log.getTarget(),
                log.getAction().name(),
                log.getTemplateId(),
                jsonObject,
                log.getModelUsed(),
                log.getResponseTime(),
                log.getDetails()
        );
    }

    @Override
    public List<AuditLog> getAllLogs() {
        String sql = "SELECT * FROM audit_log";
        return jdbcTemplate.query(sql, new AuditLogRowMapper());
    }

    @Override
    public List<AuditLog> findByUserId(String id) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE user_id = ?";
        return jdbcTemplate.query(
                sql,
                new AuditLogRowMapper(),
                id
        );
    }

    @Override
    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = """ 
                        SELECT * FROM audit_log
                        WHERE "timestamp" BETWEEN ? AND ?""";

        return jdbcTemplate.query(
                sql,
                new AuditLogRowMapper(),
                startDate,
                endDate
        );

    }

    @Override
    public List<AuditLog> findByDateRangeAndUser(LocalDateTime startDate, LocalDateTime endDate, String userId) throws SQLException {
        String sql = """ 
                        SELECT * FROM audit_log
                        WHERE user_id = ?
                        AND "timestamp" BETWEEN ? AND ?""";

        return jdbcTemplate.query(
                sql,
                new AuditLogRowMapper(),
                userId,
                startDate,
                endDate
        );
    }

    public List<PIIDailyCount> getPiiBlockedPerDay() {

        String sql = """
        SELECT DATE(timestamp) AS day, SUM(value::int) AS total
        FROM audit_log,
             jsonb_each(mask_counts)
        WHERE action = 'AI_REQUEST'
        GROUP BY day
        ORDER BY day
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new PIIDailyCount(
                        rs.getDate("day").toLocalDate(),
                        rs.getInt("total")
                )
        );
    }

    @Override
    public List<AuditLog> findByAction(ActionType action) throws SQLException {
        String sql = "SELECT * FROM audit_log WHERE action = ?";
        return jdbcTemplate.query(
                sql,
                new AuditLogRowMapper(),
                action.name()
        );
    }

    @Override
    public List<AuditLog> findSystemLogs() throws SQLException {
        String sql = """
                SELECT *
                FROM audit_log
                WHERE action::text NOT LIKE 'AI_%'""";
        return jdbcTemplate.query(
                sql,
                new AuditLogRowMapper()
        );
    }
}
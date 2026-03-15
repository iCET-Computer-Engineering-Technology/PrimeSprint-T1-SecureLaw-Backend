package com.primesprint.repository;


import com.primesprint.dto.PIIDailyCount;
import com.primesprint.model.AuditLog;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository {

    void insertAuditLog(AuditLog log) throws SQLException;

    public List<AuditLog> getAllLogs() throws SQLException;

    public List<AuditLog> findByUserId(String id) throws SQLException;

    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException;

    public List<AuditLog> findByDateRangeAndUser(LocalDateTime startDate, LocalDateTime endDate, String userId) throws SQLException;

    public List<PIIDailyCount> getPiiBlockedPerDay();
}

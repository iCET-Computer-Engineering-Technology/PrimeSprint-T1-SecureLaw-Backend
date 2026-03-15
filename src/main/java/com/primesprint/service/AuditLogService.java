package com.primesprint.service;


import com.primesprint.dto.AuditLogdto;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.model.AuditLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {

    public List<AuditLogdto> getAll() throws SQLException;

    public List<AuditLogdto> searchByUserId(String id) throws SQLException;

    public List<AuditLogdto> searchByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException;

    public List<AuditLogdto> searchByDateRangeForUser(LocalDate startDate, LocalDate endDate, String userId) throws SQLException;

    public void recordAuditLog(AuditLog log) throws SQLException;

    public List<PIIDailyCount> getPiiBlockedPerDay();
}

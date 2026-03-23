package com.primesprint.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.primesprint.dto.AuditLogDto;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.model.enums.ActionType;
import com.primesprint.model.AuditLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {

    public List<AuditLogDto> getAll() throws SQLException;

    public List<AuditLogDto> searchByUserId(String id) throws SQLException;

    public List<AuditLogDto> searchByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException;

    public List<AuditLogDto> searchByDateRangeForUser(LocalDate startDate, LocalDate endDate, String userId) throws SQLException;

    public void recordAuditLog(AuditLog log) throws SQLException, JsonProcessingException;

    public List<PIIDailyCount> getPiiMaskedPerDay();

    public List<AuditLogDto> searchByAction(ActionType action) throws SQLException;

    public List<AuditLogDto> searchSystemLogs() throws SQLException;
}

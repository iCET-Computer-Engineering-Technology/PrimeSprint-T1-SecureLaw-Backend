package com.primesprint.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.primesprint.dto.AuditLogDto;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.model.enums.ActionType;
import com.primesprint.mapper.AuditLogMapper;
import com.primesprint.model.AuditLog;
import com.primesprint.repository.AuditLogRepository;
import com.primesprint.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;

    //Introduce for AOP
    @Override
    public void recordAuditLog(AuditLog log) throws SQLException, JsonProcessingException {
        repository.insertAuditLog(log);
    }

    @Override
    public List<AuditLogDto> getAll() throws SQLException {
        List<AuditLog> logs = repository.getAllLogs();

        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();

    }

    @Override
    public List<AuditLogDto> searchByUserId(String id) throws SQLException {

        List<AuditLog> logs = repository.findByUserId(id);
        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();
    }

    @Override
    public List<AuditLogDto> searchByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {

        LocalTime localTime = LocalTime.of(23, 59, 59);
        List<AuditLog> logs = repository.findByDateRange(startDate.atStartOfDay(),endDate.atTime(localTime));
        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();

    }

    @Override
    public List<AuditLogDto> searchByDateRangeForUser(LocalDate startDate, LocalDate endDate, String userId) throws SQLException {
        LocalTime localTime = LocalTime.of(23, 59, 59);
        List<AuditLog> logs = repository.findByDateRangeAndUser(startDate.atStartOfDay(),endDate.atTime(localTime),userId);
        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();
    }

    @Override
    public List<PIIDailyCount> getPiiBlockedPerDay(){
        return repository.getPiiBlockedPerDay();
    }

    @Override
    public List<AuditLogDto> searchByAction(ActionType action) throws SQLException {

        List<AuditLog> logs = repository.findByAction(action);
        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();
    }

    @Override
    public List<AuditLogDto> searchSystemLogs() throws SQLException {
        List<AuditLog> logs = repository.findSystemLogs();
        return logs.stream()
                .map(log -> mapper.toDto(log)) //.map(mapper::toDto)
                .toList();
    }
}


package com.primesprint.service.impl;

import com.primesprint.dto.AuditLogdto;
import com.primesprint.dto.PIIDailyCount;
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
    public void recordAuditLog(AuditLog log) throws SQLException {
        repository.insertAuditLog(log);
    }

    @Override
    public List<AuditLogdto> getAll() throws SQLException {
        List<AuditLog> logs = repository.getAllLogs();

        return logs.stream()
                .map(log -> mapper.toResponse(log)) //.map(mapper::toResponse)
                .toList();

    }

    @Override
    public List<AuditLogdto> searchByUserId(String id) throws SQLException {

        List<AuditLog> logs = repository.findByUserId(id);
        return logs.stream()
                .map(log -> mapper.toResponse(log)) //.map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<AuditLogdto> searchByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {

        LocalTime localTime = LocalTime.of(23, 59, 59);
        List<AuditLog> logs = repository.findByDateRange(startDate.atStartOfDay(),endDate.atTime(localTime));
        return logs.stream()
                .map(log -> mapper.toResponse(log)) //.map(mapper::toResponse)
                .toList();

    }

    @Override
    public List<AuditLogdto> searchByDateRangeForUser(LocalDate startDate, LocalDate endDate, String userId) throws SQLException {
        LocalTime localTime = LocalTime.of(23, 59, 59);
        List<AuditLog> logs = repository.findByDateRangeAndUser(startDate.atStartOfDay(),endDate.atTime(localTime),userId);
        return logs.stream()
                .map(log -> mapper.toResponse(log)) //.map(mapper::toResponse)
                .toList();
    }

    public List<PIIDailyCount> getPiiBlockedPerDay(){
        return repository.getPiiBlockedPerDay();
    }
}


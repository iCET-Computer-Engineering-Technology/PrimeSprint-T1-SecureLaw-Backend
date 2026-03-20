package com.primesprint.controller;

import com.primesprint.dto.AuditLogDto;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.enums.ActionType;
import com.primesprint.service.AuditLogService;
import com.primesprint.service.ExportAuditService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/audit")
public class AuditLogController {

    private final AuditLogService service;
    private final ExportAuditService exportService;

    @GetMapping("/get-all")
    public List<AuditLogDto> getAll(){
        try {
            return service.getAll();
        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve audit logs.",
                    e);
        }
    }

    @GetMapping("/get-audit-by-date")
    public List<AuditLogDto> searchByDateRange(@RequestParam(name="from") LocalDate startDate,
                                            @RequestParam(name="to") LocalDate endDate){
        try {
            return service.searchByDateRange(startDate,endDate);
        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search audit logs by date range.",
                    e
            );
        }
    }

    @GetMapping("/get-audit-by-date-and-userId")
    public List<AuditLogDto> searchByDateRangeForUser(@RequestParam(name="from") LocalDate startDate,
                                                   @RequestParam(name="to") LocalDate endDate,
                                                   @RequestParam(name="id") String userId){
        try {
            return service.searchByDateRangeForUser(startDate,endDate,userId);
        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search audit logs by date range and user ID.",
                    e
            );
        }
    }

    @GetMapping("/search-by-userId/{id}")
    public List<AuditLogDto> searchByUserId(@PathVariable String id){
        try {
            return service.searchByUserId(id);
        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search audit logs by user ID.",
                    e
            );
        }
    }

    @GetMapping("/export-all-audit-logs-csv")
    public void downloadCsvAll(HttpServletResponse response){
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all_audit_logs.csv\"");

        List<AuditLogDto> logs = null;
        try {
            logs = service.getAll();
            exportService.writeAllAuditLogsToCsv(logs,response.getWriter());
        } catch (SQLException | IOException e) {
            log.error("Failed to write all audit logs to CSV", e);
            throw new RuntimeException("Failed to write all audit logs to CSV", e);
        }

    }

    @GetMapping("/export-ai-audit-logs-csv")
    public void downloadCsvAI(HttpServletResponse response){
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ai_audit_logs.csv\"");

//        List<AuditLogDto> logs = null;
        List<AuditLogDto> aiLogs =new ArrayList<>();
        try {
            aiLogs = service.searchByAction(ActionType.AI_REQUEST);

//            logs.forEach(log -> {
//                if(log.getAction() == ActionType.AI_REQUEST) {
//                    aiLogs.add(log);
//                }
//            });
            exportService.writeAIAuditLogsToCsv(aiLogs,response.getWriter());

        } catch (SQLException | IOException e) {
            log.error("Failed to write AI audit logs to CSV", e);
            throw new RuntimeException("Failed to write AI audit logs to CSV", e);
        }

    }

    @GetMapping("/export-system-audit-logs-csv")
    public void downloadCsvSystem(HttpServletResponse response){
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"system_audit_logs.csv\"");

//        List<AuditLogDto> logs = null;
        List<AuditLogDto> systemLogs =new ArrayList<>();
        try {
            systemLogs = service.searchSystemLogs();

//            logs.forEach(log -> {
//                if(log.getAction() != ActionType.AI_REQUEST) {
//                    systemLogs.add(log);
//                }
//            });

            exportService.writeSystemAuditLogsToCsv(systemLogs,response.getWriter());
        } catch (SQLException | IOException e) {
            log.error("Failed to write system audit logs to CSV", e);
            throw new RuntimeException("Failed to write system audit logs to CSV", e);
        }
    }

    @GetMapping("/pii-daily-count")
    public List<PIIDailyCount> getPiiStats(){
        return service.getPiiBlockedPerDay();
    }

}

package com.primesprint.controller;


import com.primesprint.dto.AuditLogdto;
import com.primesprint.dto.PIIDailyCount;
import com.primesprint.enums.ActionType;
import com.primesprint.service.AuditLogService;
import com.primesprint.service.ExportAuditService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/audit")
public class AuditLogController {

    final AuditLogService service;
    final ExportAuditService exportService;

    @GetMapping("/get-all")
    public List<AuditLogdto> getAll(){
        try {
            return service.getAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/get-audit-by-date")
    public List<AuditLogdto> searchByDateRange(@RequestParam(name="from") LocalDate startDate,
                                            @RequestParam(name="to") LocalDate endDate){
        try {
            return service.searchByDateRange(startDate,endDate);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/get-audit-by-date-and-userId")
    public List<AuditLogdto> searchByDateRangeForUser(@RequestParam(name="from") LocalDate startDate,
                                                   @RequestParam(name="to") LocalDate endDate,
                                                   @RequestParam(name="id") String userId){
        try {
            return service.searchByDateRangeForUser(startDate,endDate,userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/search-by-userId/{id}")
    public List<AuditLogdto> searchByUserId(@PathVariable String id){
        try {
            return service.searchByUserId(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/export-all-audit-logs-csv")
    public void downloadCsvAll(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all_audit_logs.csv\"");

        List<AuditLogdto> logs = null;
        try {
            logs = service.getAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        exportService.writeAllAuditLogsToCsv(logs,response.getWriter());
    }

    @GetMapping("/export-ai-audit-logs-csv")
    public void downloadCsvAI(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ai_audit_logs.csv\"");

        List<AuditLogdto> logs = null;
        List<AuditLogdto> aiLogs =new ArrayList<>();
        try {
            logs = service.getAll();

            logs.forEach(log -> {
                if(log.getAction() == ActionType.AI_REQUEST) {
                    aiLogs.add(log);
                }
            });

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        exportService.writeAllAuditLogsToCsv(aiLogs,response.getWriter());
    }

    @GetMapping("/export-system-audit-logs-csv")
    public void downloadCsvSystem(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"system_audit_logs.csv\"");

        List<AuditLogdto> logs = null;
        List<AuditLogdto> systemLogs =new ArrayList<>();
        try {
            logs = service.getAll();

            logs.forEach(log -> {
                if(log.getAction() != ActionType.AI_REQUEST) {
                    systemLogs.add(log);
                }
            });

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        exportService.writeAllAuditLogsToCsv(systemLogs,response.getWriter());
    }

    @GetMapping("/pii-daily-count")
    public List<PIIDailyCount> getPiiStats(){
        return service.getPiiBlockedPerDay();
    }

}

package com.primesprint.service.impl;

import com.primesprint.dto.AuditLogDto;
import com.primesprint.service.ExportAuditService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportAuditServiceImpl implements ExportAuditService {

    @Override
    public void writeAllAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Target","Action","TemplateId","Masked Counts","Model Used","Response Time","Details"};

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogDto auditLog: logs) {

                csvPrinter.printRecord(
                        auditLog.getUserId(),
                        auditLog.getTimestamp(),
                        auditLog.getTarget(),
                        auditLog.getAction(),
                        auditLog.getTemplateId(),
                        auditLog.getMaskCounts(),
                        auditLog.getModelUsed(),
                        auditLog.getResponseTime(),
                        auditLog.getDetails()
                );
            }
            csvPrinter.flush();
        }
    }

    @Override
    public void writeAIAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Action","TemplateId","Masked Counts","Model Used","Response Time","Details"};

        try (
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogDto auditLog: logs) {

                csvPrinter.printRecord(
                        auditLog.getUserId(),
                        auditLog.getTimestamp(),
                        auditLog.getAction(),
                        auditLog.getTemplateId(),
                        auditLog.getMaskCounts(),
                        auditLog.getModelUsed(),
                        auditLog.getResponseTime(),
                        auditLog.getDetails()
                );
            }
            csvPrinter.flush();

        }
    }

    @Override
    public void writeSystemAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Target","Action","Details"};

        try (
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogDto auditLog: logs) {

                csvPrinter.printRecord(
                        auditLog.getUserId(),
                        auditLog.getTimestamp(),
                        auditLog.getTarget(),
                        auditLog.getAction(),
                        auditLog.getDetails()
                );
            }
            csvPrinter.flush();

        }
    }

}
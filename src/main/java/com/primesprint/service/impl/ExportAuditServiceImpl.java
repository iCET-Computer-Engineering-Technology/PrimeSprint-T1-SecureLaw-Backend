package com.primesprint.service.impl;



import com.primesprint.dto.AuditLogdto;
import com.primesprint.service.ExportAuditService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

//@Slf4j
@Service
@RequiredArgsConstructor
public class ExportAuditServiceImpl implements ExportAuditService {

    final AuditLogServiceImpl service;

    @Override
    public void writeAllAuditLogsToCsv(List<AuditLogdto> logs, Writer writer) {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Target","Action","TemplateId","Masked Counts","Model Used","Response Time","Details"};

        try (
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogdto auditLog: logs) {

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeAIAuditLogsToCsv(List<AuditLogdto> logs, Writer writer) {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Action","TemplateId","Masked Counts","Model Used","Response Time"};

        try (
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogdto auditLog: logs) {

                csvPrinter.printRecord(
                        auditLog.getUserId(),
                        auditLog.getTimestamp(),
                        auditLog.getAction(),
                        auditLog.getTemplateId(),
                        auditLog.getMaskCounts(),
                        auditLog.getModelUsed(),
                        auditLog.getResponseTime()
                );
            }
            csvPrinter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeSystemAuditLogsToCsv(List<AuditLogdto> logs, Writer writer) {
        String[] CSV_HEADERS = {"UserId", "Timestamp","Target","Action","Details"};

        try (
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (AuditLogdto auditLog: logs) {

                csvPrinter.printRecord(
                        auditLog.getUserId(),
                        auditLog.getTimestamp(),
                        auditLog.getTarget(),
                        auditLog.getAction(),
                        auditLog.getDetails()
                );
            }
            csvPrinter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
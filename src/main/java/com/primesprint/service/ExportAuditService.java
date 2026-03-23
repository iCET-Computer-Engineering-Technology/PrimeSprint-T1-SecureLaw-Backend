package com.primesprint.service;


import com.primesprint.dto.AuditLogDto;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface ExportAuditService {

    public void writeAllAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException;

    public void writeAIAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException;

    public void writeSystemAuditLogsToCsv(List<AuditLogDto> logs, Writer writer) throws IOException;

}

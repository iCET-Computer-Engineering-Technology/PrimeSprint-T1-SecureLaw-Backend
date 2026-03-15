package com.primesprint.service;


import com.primesprint.dto.AuditLogdto;

import java.io.Writer;
import java.util.List;

public interface ExportAuditService {

    public void writeAllAuditLogsToCsv(List<AuditLogdto> logs, Writer writer);

    public void writeAIAuditLogsToCsv(List<AuditLogdto> logs, Writer writer);

    public void writeSystemAuditLogsToCsv(List<AuditLogdto> logs, Writer writer);

}

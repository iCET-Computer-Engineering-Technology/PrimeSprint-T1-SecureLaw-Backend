package com.primesprint.service.fallback;

import com.primesprint.model.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;

@Component
@Slf4j
public class AuditFallbackHandler {

    private static final String FILE = "audit-fallback.log";

    public void handle(AuditLog logObj, Exception ex) {
        log.error("Writing audit to fallback store", ex);

        try (FileWriter fw = new FileWriter(FILE, true)) {
            fw.write(logObj.toString());
            fw.write(System.lineSeparator());
        } catch (IOException ioEx) {
            log.error("CRITICAL: fallback failed", ioEx);
        }
    }
}
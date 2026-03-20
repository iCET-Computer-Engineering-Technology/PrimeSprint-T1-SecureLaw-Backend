package com.primesprint.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.primesprint.custom_annotation.Auditable;
import com.primesprint.enums.ActionType;
import com.primesprint.model.AuditLog;
import com.primesprint.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class SystemAuditAspect {
    private final AuditLogService auditService;
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAuditAspect.class);

    @AfterReturning(value = "@annotation(auditable)")
    public void logSystemAudit(JoinPoint joinPoint, Auditable auditable){

        ActionType action = auditable.action();

        String userId = "AdminORUser"; //Hardcoded value : Should come from logging service

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName(); //Annotated method name

        String target = className + "." + methodName;

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID().toString()) //generated UUID from database later
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .target(target)
                .action(action)
                .templateId(null)
                .maskCounts(null)
                .modelUsed(null)
                .responseTime(null)
                .details(userId + " executed " + methodName)
                .build();

        try {
            auditService.recordAuditLog(log);
        } catch (Exception e) {
            LOGGER.error("Failed to record system audit log", e);
        }
    }
}

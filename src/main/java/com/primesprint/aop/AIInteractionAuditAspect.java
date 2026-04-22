package com.primesprint.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.primesprint.custom_annotation.AIInteractionAuditable;
import com.primesprint.model.AuditLog;
import com.primesprint.service.AuditLogService;
import com.primesprint.service.fallback.AuditFallbackHandler;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AIInteractionAuditAspect {

    private final AuditLogService auditService;
    private static final Logger logger = LoggerFactory.getLogger(AIInteractionAuditAspect.class);
    private final MeterRegistry meterRegistry;
    private final AuditFallbackHandler fallbackHandler;

    @Around("@annotation(aIAuditable)")
    public Object logAIAudit(ProceedingJoinPoint joinPoint, AIInteractionAuditable aIAuditable) throws Throwable {

        long startTime = System.currentTimeMillis();

        Object result = null;
        Throwable methodException = null;

        try {
            result = joinPoint.proceed();
            return result;

        } catch (Throwable ex) {
            methodException = ex;
            throw ex;
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;

            String templateId = null;
            String modelUsed = null;
            Map<String, Integer> maskCounts = null;

            if (result instanceof AIResult aiResult) {

                templateId = aiResult.getTemplateId();
                maskCounts = aiResult.getMaskCounts();
                modelUsed = aiResult.getModelUsed();
            }

            //Builder Design Pattern
            AuditLog log = AuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .userId("U0001") //From Authentication service , Spring security, SecurityContext
                    .timestamp(LocalDateTime.now())
                    .target(null)
                    .action(aIAuditable.action())
                    .templateId(templateId) //From AIService
                    .maskCounts(maskCounts) //From AI Service
                    .modelUsed(modelUsed) //From AI Service
                    .responseTime(responseTime)
                    .details(methodException != null ? "Fail : "+methodException.getMessage() : "success")
                    .build();

            try {
                auditService.recordAuditLog(log);
            } catch (Exception e) {
                logger.error("Audit persistence failed for AI interaction", e);

                meterRegistry.counter("audit.failure.count").increment();
                fallbackHandler.handle(log, e);
            }

        }
    }
}


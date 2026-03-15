package com.primesprint.aop;

import com.primesprint.common.AIResult;
import com.primesprint.custom_annotation.AIInteractionAuditable;
import com.primesprint.model.AuditLog;
import com.primesprint.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AIInteractionAuditAspect {
    private final AuditLogService auditService;

    @Around("@annotation(aIAuditable)")
    public Object logAIAudit(ProceedingJoinPoint joinPoint, AIInteractionAuditable aIAuditable) throws Throwable {

        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed(); //returning object at the end of annotated method

        long responseTime = System.currentTimeMillis() - startTime;

        String templateId = null;
        String modelUsed = null;
        Map<String, Integer> maskCounts = null;

        if (result instanceof AIResult aiResult) {

            templateId = aiResult.getTemplateId();
            maskCounts = aiResult.getMaskCounts();
            modelUsed = aiResult.getModelUsed();
        }

        //Builder Design Pattern used
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID().toString()) //generated UUID from database later
                .userId("U0001") //From Authentication service , Spring security, SecurityContext
                .timestamp(LocalDateTime.now())
                .target(null)
                .action(aIAuditable.action())
                .templateId(templateId) //From AIService
                .maskCounts(maskCounts) //From AI Service
                .modelUsed(modelUsed) //From AI Service
                .responseTime(responseTime)
                .details(null)
                .build();

        auditService.recordAuditLog(log);

        return result;

    }
}


package com.primesprint.service.impl;

import com.primesprint.model.AIResponseResult;
import com.primesprint.custom_annotation.AIInteractionAuditable;
import com.primesprint.custom_annotation.Auditable;
import com.primesprint.model.enums.ActionType;
import com.primesprint.service.TestAuditService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TestAuditServiceImpl implements TestAuditService {
    @Override
    @Auditable(action = ActionType.TEST_ACTION)
    public String testAuditMethod() {
        return "Test Executed";
    }

    @Override
    @AIInteractionAuditable(action = ActionType.AI_REQUEST)
    public AIResponseResult testAIRequestAuditMethod() {
        return AIResponseResult.builder()
                .templateId("TEMPLATE_123")
                .modelUsed("GPT-4")
                .maskCounts(Map.of("MASK1", 2, "MASK2", 3))
                .build();
    }
}

package com.primesprint.controller;


import com.primesprint.model.AIResponseResult;
import com.primesprint.service.TestAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("audit/test")
public class TestAuditController {

    private final TestAuditService testService;

    @PostMapping("/audit-test")
    public String runTest(){
        return testService.testAuditMethod();
    }

    @PostMapping("/audit-ai-test")
    public AIResponseResult runAITest(){
        return testService.testAIRequestAuditMethod();
    }
}

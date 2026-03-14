package com.primesprint.pii.controller;

import com.primesprint.pii.dto.PiiDetectRequest;
import com.primesprint.pii.dto.SensitiveDataItem;
import com.primesprint.pii.service.PiiDetectService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/pii")
public class PiiDetectController {

    private final PiiDetectService piiDetectService;

    public PiiDetectController(PiiDetectService piiDetectService) {
        this.piiDetectService = piiDetectService;
    }

    @PostMapping(value = "/detect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SensitiveDataItem>> detect(@Valid @RequestBody PiiDetectRequest request) {
        List<SensitiveDataItem> result = piiDetectService.detect(request);
        return ResponseEntity.ok(result);
    }
}


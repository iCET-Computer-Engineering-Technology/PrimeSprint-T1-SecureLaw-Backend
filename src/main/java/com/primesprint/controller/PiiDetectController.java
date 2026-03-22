package com.primesprint.controller;


import com.primesprint.model.dto.SensitiveDataItem;
import com.primesprint.model.dto.request.PiiDetectRequest;
import com.primesprint.service.PiiDetectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/pii")
public class PiiDetectController {

    private final PiiDetectService piiDetectService;

    @PostMapping(value = "/detect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SensitiveDataItem>> detect(@Valid @RequestBody PiiDetectRequest request) {
        List<SensitiveDataItem> result = piiDetectService.detect(request);
        return ResponseEntity.ok(result);
    }
}


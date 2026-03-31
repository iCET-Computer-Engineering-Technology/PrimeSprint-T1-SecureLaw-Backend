package com.primesprint.controller;

import com.primesprint.model.dto.request.ExternalAiRequest;
import com.primesprint.model.dto.response.ExternalAiResponse;
import com.primesprint.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/external/ai")
@RequiredArgsConstructor
public class ExternalAiController {

    final GeminiService geminiService;

    @PostMapping("/process")
    public ExternalAiResponse process(@RequestBody ExternalAiRequest externalAiRequest){
        return geminiService.process(externalAiRequest);
    }

}

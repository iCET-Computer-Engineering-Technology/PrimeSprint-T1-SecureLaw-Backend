package com.primesprint.service;


import com.primesprint.model.dto.request.ExternalAiRequest;
import com.primesprint.model.dto.response.ExternalAiResponse;

public interface GeminiService {
    ExternalAiResponse process(ExternalAiRequest externalAiRequest);
}

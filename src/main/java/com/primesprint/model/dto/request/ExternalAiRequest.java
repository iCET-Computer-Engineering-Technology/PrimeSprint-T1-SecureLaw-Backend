package com.primesprint.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAiRequest {
    private String requestId;
    private String provider;
    private String maskedPrompt;
    private String maskedDocument;
    private Map<String, String> tokenMappings;
    private RequestOptions options;
    private Long timeoutMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestOptions {
        private String model;
        private Integer maxTokens;
        private Double temperature;
    }
}

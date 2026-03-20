package com.primesprint.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MaskResponse {
    private UUID requestId;
    private String maskedDocument;
    private String maskedPrompt;
    private UUID mappingId;
    private Map<String,String> tokenMappings;
}

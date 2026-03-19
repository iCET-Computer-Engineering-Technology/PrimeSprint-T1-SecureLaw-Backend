package com.primesprint.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RehydrateRequest {
    private UUID mappingId;
    private String tokenizedResponse;
    private Map<String,String> tokenMappings;
}

package com.primesprint.model.dto.request;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RehydrateRequest {
    private UUID mappingId;
    private String tokenizedResponse;
    private Map<String,String> tokenMappings;
}

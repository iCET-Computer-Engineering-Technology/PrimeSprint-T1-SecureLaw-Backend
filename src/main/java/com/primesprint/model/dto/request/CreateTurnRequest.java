package com.primesprint.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTurnRequest {
    private String userPrompt;
    private String aiResponse;
    private String modelName;
    private Long latencyMs;
}

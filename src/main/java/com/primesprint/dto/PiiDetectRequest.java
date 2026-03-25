package com.primesprint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PiiDetectRequest {

    @NotBlank
    private String requestId;
    private String documentExtractedContent;
    private String userPrompt;
}


package com.primesprint.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PiiDetectRequest {

    @NotBlank
    private String requestId;
    private String documentExtractedContent;
    private String userPrompt;
}


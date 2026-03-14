package com.primesprint.pii.dto;

import jakarta.validation.constraints.NotBlank;

public class PiiDetectRequest {
    @NotBlank
    private String requestId;

    private String documentExtractedContent;
    private String userPrompt;

    public PiiDetectRequest() {
    }

    public PiiDetectRequest(String requestId, String documentExtractedContent, String userPrompt) {
        this.requestId = requestId;
        this.documentExtractedContent = documentExtractedContent;
        this.userPrompt = userPrompt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDocumentExtractedContent() {
        return documentExtractedContent;
    }

    public void setDocumentExtractedContent(String documentExtractedContent) {
        this.documentExtractedContent = documentExtractedContent;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }
}


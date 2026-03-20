package com.primesprint.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.UUID;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class MaskRequest {
    private UUID requestId;
    private String prompt;
    private String document;
    private ArrayList<SensitiveData> sensitiveData;

    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    public static class SensitiveData{
        private String type;
        private String value;
        private String source;
        private Integer start;
        private Integer end;
    }
}

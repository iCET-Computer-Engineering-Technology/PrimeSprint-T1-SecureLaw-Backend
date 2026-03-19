package com.primesprint.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RehydrateResponse {
    private UUID mappingId;
    private String finalText;
    private ArrayList<Warning> warnings;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Warning{
        private String type;
        private String token;
        private String message;
    }
}

package com.primesprint.model.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RehydrateResponse {
    private UUID mappingId;
    private String finalText;
    private List<Warning> warnings;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Warning{
        private String type;
        private String token;
        private String message;
    }
}

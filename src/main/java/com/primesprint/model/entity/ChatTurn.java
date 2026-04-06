package com.primesprint.model.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatTurn {
    private UUID id;
    private UUID chatId;
    private String userPrompt;
    private String aiResponse;
    private String modelName;
    private Long latencyMs;
    private LocalDateTime createdAt;
}

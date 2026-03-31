package com.primesprint.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Data
public class ChatMessageResponse {
    private String conversationId;
    private  String message;
    private String aiResponse;
    private LocalDateTime timeStamp;


}

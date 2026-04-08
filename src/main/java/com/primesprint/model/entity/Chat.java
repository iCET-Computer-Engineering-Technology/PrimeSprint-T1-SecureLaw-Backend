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
public class Chat {
    private UUID id;
    private UUID profileId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

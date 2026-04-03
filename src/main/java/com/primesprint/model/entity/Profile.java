package com.primesprint.model.entity;

import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Profile {
    private UUID id;
    private UUID userId;
    private String displayName;
    private Timestamp createdAt;
}

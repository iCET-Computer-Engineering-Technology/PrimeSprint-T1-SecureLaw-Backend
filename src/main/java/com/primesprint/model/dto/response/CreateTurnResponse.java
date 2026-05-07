package com.primesprint.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CreateTurnResponse {
    private UUID turnId;
}

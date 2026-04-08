package com.primesprint.repository;

import com.primesprint.model.entity.ChatTurn;

import java.util.UUID;

public interface ChatTurnRepository {
    UUID save(ChatTurn turn);
}

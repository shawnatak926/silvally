package com.silvally.service;

import com.silvally.dto.response.MutationResult;

import java.util.UUID;

public interface SchoolTurnService {
    MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey);
}

package com.silvally.service;

import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.YearTransitionView;

import java.util.UUID;

public interface SchoolTurnService {
    MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey);

    YearTransitionView getYearTransition(UUID playerId);

    MutationResult chooseYearTransition(UUID playerId, long expectedVersion, String idempotencyKey,
                                        YearTransitionRequest request);
}

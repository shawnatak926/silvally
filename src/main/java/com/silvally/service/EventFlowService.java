package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.MutationResult;
import java.util.List;
import java.util.UUID;

public interface EventFlowService {
    List<EventView> list(UUID playerId);
    EventView detail(UUID playerId, String eventCode);
    MutationResult choose(UUID playerId, String eventCode, long expectedVersion,
                          String idempotencyKey, ChooseEventRequest request);
}

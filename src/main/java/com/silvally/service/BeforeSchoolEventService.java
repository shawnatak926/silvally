package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.Player;

public interface BeforeSchoolEventService {
    String currentEvent(Player player);
    EventView view(Player player, String eventCode);
    MutationResult apply(Player player, String eventCode, ChooseEventRequest request);
}

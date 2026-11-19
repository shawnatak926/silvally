package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.*;
import com.silvally.entity.Player;
import java.util.*;

public interface EventService {
    List<EventView> list(UUID playerId);
    EventView detail(UUID playerId, String eventCode);
    MutationResult choose(UUID playerId, String eventCode, long version, String key, ChooseEventRequest request);
    void requireNoPendingEvent(Player player);
}

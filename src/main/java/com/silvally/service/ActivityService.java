package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.PerformActionRequest;
import com.silvally.dto.response.AvailableAction;
import com.silvally.dto.response.CatalogEntry;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.ItemStack;
import com.silvally.dto.response.MutationResult;

import java.util.List;
import java.util.UUID;

public interface ActivityService {
    List<CatalogEntry> getCatalog(UUID playerId, String kind);

    List<EventView> getEvents(UUID playerId);

    EventView getEvent(UUID playerId, String eventCode);

    MutationResult chooseEvent(UUID playerId, String eventCode, long expectedVersion,
                               String idempotencyKey, ChooseEventRequest request);

    List<ItemStack> getInventory(UUID playerId);

    MutationResult useItem(UUID playerId, String itemCode, long expectedVersion, String idempotencyKey);

    List<AvailableAction> getActions(UUID playerId);

    MutationResult performAction(UUID playerId, long expectedVersion, String idempotencyKey,
                                 PerformActionRequest request);
}

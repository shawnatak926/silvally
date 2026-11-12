package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.PerformActionRequest;
import com.silvally.dto.response.AvailableAction;
import com.silvally.dto.response.CatalogEntry;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.ItemStack;
import com.silvally.dto.response.MutationResult;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.EventChoiceRepository;
import com.silvally.repository.EventDefRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActivityServiceImpl implements ActivityService {

    private final EventFlowService admissionEvents;
    private final PlayerRepository playerRepository;
    private final EventDefRepository eventDefRepository;
    private final EventChoiceRepository eventChoiceRepository;
    private final CatalogDefRepository catalogDefRepository;
    private final CommandExecutor commandExecutor;

    public ActivityServiceImpl(EventFlowService admissionEvents, PlayerRepository playerRepository,
                               EventDefRepository eventDefRepository,
                               EventChoiceRepository eventChoiceRepository,
                               CatalogDefRepository catalogDefRepository,
                               CommandExecutor commandExecutor) {
        this.admissionEvents = admissionEvents;
        this.playerRepository = playerRepository;
        this.eventDefRepository = eventDefRepository;
        this.eventChoiceRepository = eventChoiceRepository;
        this.catalogDefRepository = catalogDefRepository;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public List<CatalogEntry> getCatalog(UUID playerId, String kind) {
        throw new UnsupportedOperationException("TODO: 카탈로그 조회");
    }

    @Override
    public List<EventView> getEvents(UUID playerId) {
        return admissionEvents.list(playerId);
    }

    @Override
    public EventView getEvent(UUID playerId, String eventCode) {
        return admissionEvents.detail(playerId, eventCode);
    }

    @Override
    @Transactional
    public MutationResult chooseEvent(UUID playerId, String eventCode, long expectedVersion,
                                      String idempotencyKey, ChooseEventRequest request) {
        return admissionEvents.choose(playerId, eventCode, expectedVersion, idempotencyKey, request);
    }

    @Override
    public List<ItemStack> getInventory(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 인벤토리");
    }

    @Override
    @Transactional
    public MutationResult useItem(UUID playerId, String itemCode, long expectedVersion, String idempotencyKey) {
        throw new UnsupportedOperationException("TODO: 아이템 사용");
    }

    @Override
    public List<AvailableAction> getActions(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 가용 행동 목록");
    }

    @Override
    @Transactional
    public MutationResult performAction(UUID playerId, long expectedVersion, String idempotencyKey,
                                        PerformActionRequest request) {
        throw new UnsupportedOperationException("TODO: 행동 실행");
    }
}

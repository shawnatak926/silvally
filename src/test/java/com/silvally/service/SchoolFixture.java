package com.silvally.service;

import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.entity.GrowthProgress;
import com.silvally.entity.Player;
import com.silvally.repository.PlayerRepository;
import com.silvally.support.TestStats;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
class SchoolFixture {

    private final ActivityService events;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final TransactionTemplate tx;

    SchoolFixture(PlayerService playerService, PlayerRepository playerRepository,
                  PlatformTransactionManager transactionManager, ActivityService events) {
        this.events = events;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    UUID enrolledStudent() {
        PlayerView created = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("student", null));
        modify(created.id(), player -> {
            TestStats.pin(player);
            player.startSchool(SchoolRules.AP_PER_TURN);
        });
        events.chooseEvent(created.id(), "FOCUS_Y1_S1", version(created.id()), UUID.randomUUID().toString(),
                new com.silvally.dto.request.ChooseEventRequest("COMMUNICATION"));
        return created.id();
    }

    void modify(UUID playerId, Consumer<Player> change) {
        tx.executeWithoutResult(status -> change.accept(playerRepository.findById(playerId).orElseThrow()));
    }

    void addStats(UUID playerId, Map<String, Integer> delta) {
        modify(playerId, player -> player.getStats().apply(delta));
    }

    /** 정수 능력치와 소수 누적분을 합친 1/1000점 단위 값. */
    int milli(UUID playerId, String stat) {
        Player player = load(playerId);
        return player.getStats().get(stat) * GrowthProgress.UNIT + player.getGrowthProgress().get(stat);
    }

    Player load(UUID playerId) {
        return playerRepository.findById(playerId).orElseThrow();
    }

    long version(UUID playerId) {
        return load(playerId).getVersion();
    }

    MutationResult advance(UUID playerId) {
        return playerService.advanceTurn(playerId, version(playerId), UUID.randomUUID().toString());
    }

    MutationResult completeCalendarChoices(UUID playerId) {
        MutationResult last = null;
        for (var event : events.getEvents(playerId)) {
            if (!event.required()) continue;
            String choice = event.code().startsWith("FOCUS_") ? "COMMUNICATION"
                : event.code().startsWith("VACATION_") ? "REST" : event.code().equals("J01") ? "DOMESTIC" : null;
            if (choice != null) last = events.chooseEvent(playerId, event.code(), version(playerId), UUID.randomUUID().toString(),
                    new com.silvally.dto.request.ChooseEventRequest(choice));
        }
        return last;
    }

    MutationResult advanceTimes(UUID playerId, int times) {
        MutationResult last = null;
        for (int i = 0; i < times; i++) {
            completeCalendarChoices(playerId);
            last = advance(playerId);
            if ("VACATION_REQUIRED".equals(last.resultCode())) last = completeCalendarChoices(playerId);
        }
        return last;
    }
}

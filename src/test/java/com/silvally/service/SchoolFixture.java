package com.silvally.service;

import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.entity.Player;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
class SchoolFixture {

    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final TransactionTemplate tx;

    SchoolFixture(PlayerService playerService, PlayerRepository playerRepository,
                  PlatformTransactionManager transactionManager) {
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    UUID enrolledStudent() {
        PlayerView created = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("student", null));
        modify(created.id(), player -> player.startSchool(SchoolRules.AP_PER_TURN));
        return created.id();
    }

    void modify(UUID playerId, Consumer<Player> change) {
        tx.executeWithoutResult(status -> change.accept(playerRepository.findById(playerId).orElseThrow()));
    }

    void addStats(UUID playerId, Map<String, Integer> delta) {
        modify(playerId, player -> player.getStats().apply(delta));
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

    MutationResult advanceTimes(UUID playerId, int times) {
        MutationResult last = null;
        for (int i = 0; i < times; i++) {
            last = advance(playerId);
        }
        return last;
    }
}

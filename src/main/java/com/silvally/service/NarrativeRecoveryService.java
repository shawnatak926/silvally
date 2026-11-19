package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.NarrativeRetryResult;
import com.silvally.entity.NarrativeStatus;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NarrativeRecoveryService {
    static final int MAX_ATTEMPTS = 5;

    private final PlayerRepository players;
    private final LifeNarrativeRepository narratives;
    private final NarrativeService narrativeService;

    public NarrativeRecoveryService(PlayerRepository players, LifeNarrativeRepository narratives,
                                    NarrativeService narrativeService) {
        this.players = players;
        this.narratives = narratives;
        this.narrativeService = narrativeService;
    }

    public NarrativeRetryResult retry(UUID playerId) {
        if (!players.existsById(playerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다.");
        }
        List<Long> targets = narratives.findRetryTargets(playerId, MAX_ATTEMPTS);
        targets.forEach(narrativeService::write);
        long saved = narratives.findAllById(targets).stream()
                .filter(n -> n.getStatus() == NarrativeStatus.SAVED)
                .count();
        return new NarrativeRetryResult(targets.size(), (int) saved, targets.size() - (int) saved);
    }
}

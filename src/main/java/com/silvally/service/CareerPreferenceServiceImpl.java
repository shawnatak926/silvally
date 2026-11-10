package com.silvally.service;

import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.CareerPreferenceMutation;
import com.silvally.dto.response.CareerPreferenceView;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareerPreferenceServiceImpl implements CareerPreferenceService {

    private final PlayerRepository playerRepository;
    private final CommandLogRepository commandLogRepository;
    private final CommandExecutor commandExecutor;

    public CareerPreferenceServiceImpl(PlayerRepository playerRepository,
                                       CommandLogRepository commandLogRepository,
                                       CommandExecutor commandExecutor) {
        this.playerRepository = playerRepository;
        this.commandLogRepository = commandLogRepository;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public CareerPreferenceView getPreference(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 저장된 추천 조회");
    }

    @Override
    @Transactional
    public CareerPreferenceMutation select(UUID playerId, long expectedVersion, String idempotencyKey,
                                           SelectCareerPreferenceRequest request) {
        throw new UnsupportedOperationException("TODO: 희망 직무 확정·변경");
    }
}

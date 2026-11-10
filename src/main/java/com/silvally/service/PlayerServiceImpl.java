package com.silvally.service;

import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.DeleteResult;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.dto.response.ProgressView;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final CommandLogRepository commandLogRepository;
    private final CommandExecutor commandExecutor;

    public PlayerServiceImpl(PlayerRepository playerRepository,
                             CommandLogRepository commandLogRepository,
                             CommandExecutor commandExecutor) {
        this.playerRepository = playerRepository;
        this.commandLogRepository = commandLogRepository;
        this.commandExecutor = commandExecutor;
    }

    @Override
    @Transactional
    public PlayerView create(String idempotencyKey, CreatePlayerRequest request) {
        throw new UnsupportedOperationException("TODO: 회차 생성");
    }

    @Override
    public PlayerView get(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 회차 조회");
    }

    @Override
    @Transactional
    public PlayerView rename(UUID playerId, long expectedVersion, String idempotencyKey, RenamePlayerRequest request) {
        throw new UnsupportedOperationException("TODO: 이름 변경");
    }

    @Override
    @Transactional
    public DeleteResult delete(UUID playerId, long expectedVersion) {
        throw new UnsupportedOperationException("TODO: 회차 삭제");
    }

    @Override
    public ProgressView getProgress(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 진행 현황 조회");
    }

    @Override
    public HistoryPage getHistory(UUID playerId, long afterId, int limit) {
        throw new UnsupportedOperationException("TODO: 이력 조회");
    }

    @Override
    @Transactional
    public MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey) {
        throw new UnsupportedOperationException("TODO: 턴 종료");
    }
}

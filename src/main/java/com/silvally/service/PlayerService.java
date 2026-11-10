package com.silvally.service;

import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.DeleteResult;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.dto.response.ProgressView;

import java.util.UUID;

public interface PlayerService {
    PlayerView create(String idempotencyKey, CreatePlayerRequest request);

    PlayerView get(UUID playerId);

    PlayerView rename(UUID playerId, long expectedVersion, String idempotencyKey, RenamePlayerRequest request);

    DeleteResult delete(UUID playerId, long expectedVersion);

    ProgressView getProgress(UUID playerId);

    HistoryPage getHistory(UUID playerId, long afterId, int limit);

    MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey);
}

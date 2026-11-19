package com.silvally.service;

import com.silvally.dto.response.HistoryPage;
import com.silvally.entity.CommandLog;
import com.silvally.entity.Player;

import java.util.UUID;

public interface HistoryService {
    HistoryPage getHistory(UUID playerId, long afterId, int limit);

    CommandLog recordScene(Player player, SceneRecord scene);
}

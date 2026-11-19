package com.silvally.service;

import com.silvally.dto.response.HistoryPage;

import java.util.UUID;

public interface HistoryService {
    HistoryPage getHistory(UUID playerId, long afterId, int limit);
}

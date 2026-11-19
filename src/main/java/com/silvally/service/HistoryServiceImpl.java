package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.HistoryPage;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.HistoryRow;
import com.silvally.repository.PlayerRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HistoryServiceImpl implements HistoryService {

    private final PlayerRepository playerRepository;
    private final CommandLogRepository commandLogRepository;

    public HistoryServiceImpl(PlayerRepository playerRepository, CommandLogRepository commandLogRepository) {
        this.playerRepository = playerRepository;
        this.commandLogRepository = commandLogRepository;
    }

    @Override
    public HistoryPage getHistory(UUID playerId, long afterId, int limit) {
        if (!playerRepository.existsById(playerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다.");
        }
        List<HistoryRow> rows = commandLogRepository.findHistory(playerId, afterId, Limit.of(limit + 1));
        boolean hasNext = rows.size() > limit;
        List<HistoryPage.Item> items = rows.stream().limit(limit).map(HistoryServiceImpl::toItem).toList();
        Long nextAfterId = hasNext ? items.get(items.size() - 1).logId() : null;
        return new HistoryPage(items, nextAfterId);
    }

    private static HistoryPage.Item toItem(HistoryRow row) {
        return new HistoryPage.Item(row.logId(), row.year(), row.turn(), row.beforeSchoolDay(),
                row.kind(), row.subjectCode(), row.choiceCode(), row.resultCode());
    }
}

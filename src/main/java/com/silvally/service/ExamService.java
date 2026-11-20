package com.silvally.service;

import com.silvally.dto.response.ExamResultView;
import com.silvally.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamService {

    /** 정규 턴 종료 시 시험 준비도를 쌓고, 평가 턴이면 점수를 내어 성적 G를 갱신한다. */
    Optional<ExamResultView> onTurnEnd(Player player, List<StatChange> changes);

    List<ExamResultView> results(UUID playerId);
}

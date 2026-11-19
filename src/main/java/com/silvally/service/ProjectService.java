package com.silvally.service;

import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.ProjectView;
import com.silvally.entity.Player;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectView> getProjects(UUID playerId);

    MutationResult start(UUID playerId, String projectCode, long expectedVersion, String idempotencyKey);

    MutationResult work(UUID playerId, String projectCode, long expectedVersion, String idempotencyKey);

    MutationResult registerPortfolio(UUID playerId, String projectCode, long expectedVersion, String idempotencyKey);

    /** 다음 턴으로 넘어갈 때 기간이 끝난 진행 중 프로젝트를 만료 처리하고 만료된 코드를 돌려준다. */
    List<String> expireOverdue(Player player, int nextAbsoluteTurn);
}

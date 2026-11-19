package com.silvally.service;

import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;
import com.silvally.entity.Player;

import java.util.List;
import java.util.UUID;

public interface CareerService {
    List<ApplicationView> getApplications(UUID playerId);

    ApplicationMutation apply(UUID playerId, long expectedVersion, String idempotencyKey, ApplyJobRequest request);

    ApplicationMutation proceedStep(UUID playerId, UUID applicationId, long expectedVersion,
                                    String idempotencyKey, JobStepRequest request);

    EndingMutation graduate(UUID playerId, long expectedVersion, String idempotencyKey,
                            FinalizeGraduationRequest request);

    EndingView getEnding(UUID playerId);

    /** 다음 턴으로 넘어갈 때 마감이 지난 진행 중 지원을 만료 처리하고 만료된 기업 코드를 돌려준다. */
    List<String> expireOverdue(Player player, int nextAbsoluteTurn);
}

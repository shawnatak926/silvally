package com.silvally.service;

import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;

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
}

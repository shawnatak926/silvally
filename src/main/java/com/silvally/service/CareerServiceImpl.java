package com.silvally.service;

import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;
import com.silvally.repository.JobApplicationRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareerServiceImpl implements CareerService {

    private final PlayerRepository playerRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CommandExecutor commandExecutor;

    public CareerServiceImpl(PlayerRepository playerRepository,
                             JobApplicationRepository jobApplicationRepository,
                             CommandExecutor commandExecutor) {
        this.playerRepository = playerRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public List<ApplicationView> getApplications(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 지원 목록");
    }

    @Override
    @Transactional
    public ApplicationMutation apply(UUID playerId, long expectedVersion, String idempotencyKey,
                                     ApplyJobRequest request) {
        throw new UnsupportedOperationException("TODO: 기업 지원·서류 평가");
    }

    @Override
    @Transactional
    public ApplicationMutation proceedStep(UUID playerId, UUID applicationId, long expectedVersion,
                                           String idempotencyKey, JobStepRequest request) {
        throw new UnsupportedOperationException("TODO: 코딩·면접·결과 단계");
    }

    @Override
    @Transactional
    public EndingMutation graduate(UUID playerId, long expectedVersion, String idempotencyKey,
                                   FinalizeGraduationRequest request) {
        throw new UnsupportedOperationException("TODO: 졸업 판정");
    }

    @Override
    public EndingView getEnding(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 엔딩 조회");
    }
}

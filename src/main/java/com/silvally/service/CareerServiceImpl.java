package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.JobApplication;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.JobApplicationRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareerServiceImpl implements CareerService {

    private final PlayerRepository playerRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CommandExecutor commandExecutor;
    private final GraduationPolicy graduationPolicy;
    private final GraduationSummaryBuilder summaryBuilder;
    private final CatalogDefRepository catalog;
    private final ObjectMapper objectMapper;

    public CareerServiceImpl(PlayerRepository playerRepository,
                             JobApplicationRepository jobApplicationRepository,
                             CommandExecutor commandExecutor,
                             GraduationPolicy graduationPolicy,
                             GraduationSummaryBuilder summaryBuilder,
                             CatalogDefRepository catalog,
                             ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.commandExecutor = commandExecutor;
        this.graduationPolicy = graduationPolicy;
        this.summaryBuilder = summaryBuilder;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
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
        EndingView[] ending = new EndingView[1];
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("GRADUATE", request.path(), request, EndingMutation.class),
                player -> ending[0] = finalizeGraduation(player, request),
                player -> new EndingMutation(ending[0], player.view()));
    }

    @Override
    public EndingView getEnding(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        if (player.getPhase() != Phase.ENDED || player.getEndingCode() == null) {
            throw new BusinessException(ErrorCode.ENDING_NOT_READY);
        }
        if (player.getEndingJson() == null) {
            // 졸업 요약 없이 끝난 회차(입학 탈락 등)는 엔딩 코드만 알려 준다.
            return endingView(player, player.getEndingCode(), null, null, Map.of());
        }
        return objectMapper.readValue(player.getEndingJson(), EndingView.class);
    }

    private EndingView finalizeGraduation(Player player, FinalizeGraduationRequest request) {
        if (player.getPhase() != Phase.GRAD_READY) {
            throw new BusinessException(ErrorCode.WRONG_PHASE, "졸업 단계에서만 요청할 수 있습니다.");
        }
        GraduationPath requested = GraduationPath.parse(request.path());
        List<JobApplication> applications = jobApplicationRepository.findByPlayerIdOrderByCompanyCodeAsc(player.getId());
        GraduationPolicy.Decision decision = graduationPolicy.decide(requested, request.applicationId(), player,
                applications, summaryBuilder.portfolioCount(player.getId()));

        settleOffers(applications, decision.accepted());
        EndingView ending = endingView(player, decision.endingCode(), decision.path().name(),
                decision.accepted(), summaryBuilder.build(player, decision, requested));
        player.finishGraduation(decision.endingCode(), objectMapper.writeValueAsString(ending));
        return ending;
    }

    /** 선택한 오퍼만 수락하고, 남은 오퍼는 모두 거절 처리한다. */
    private static void settleOffers(List<JobApplication> applications, JobApplication accepted) {
        for (JobApplication application : applications) {
            if (!application.isOffer()) {
                continue;
            }
            if (application == accepted) {
                application.accept();
            } else {
                application.decline();
            }
        }
    }

    private EndingView endingView(Player player, String endingCode, String selectedPath,
                                  JobApplication accepted, Map<String, Object> summary) {
        CatalogDef def = catalog.findById(new CatalogDef.Id(player.getRulesVersion(), CatalogKind.ENDING.name(), endingCode))
                .orElse(null);
        String title = def == null ? endingCode : def.getTitle();
        String body = def == null ? "" : objectMapper.readTree(def.getConfigJson()).path("body").asString("");
        return new EndingView(endingCode, title, body, selectedPath,
                accepted == null ? null : accepted.getCompanyCode(),
                accepted == null ? null : accepted.getRole(), summary);
    }
}

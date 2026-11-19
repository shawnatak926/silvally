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
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.ApplicationStatus;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareerServiceImpl implements CareerService {
    static final int STEP_AP = 1;
    private static final List<ApplicationStatus> IN_PROGRESS = List.of(
            ApplicationStatus.DOC_PASSED, ApplicationStatus.CODE_PASSED, ApplicationStatus.INTERVIEW_PASSED);

    private final PlayerRepository playerRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CommandExecutor commandExecutor;
    private final GraduationPolicy graduationPolicy;
    private final GraduationSummaryBuilder summaryBuilder;
    private final CatalogDefRepository catalog;
    private final ObjectMapper objectMapper;
    private final SchoolRules schoolRules;
    private final ProjectRules projectRules;
    private final RecruitmentRules recruitmentRules;

    public CareerServiceImpl(PlayerRepository playerRepository,
                             JobApplicationRepository jobApplicationRepository,
                             CommandExecutor commandExecutor,
                             GraduationPolicy graduationPolicy,
                             GraduationSummaryBuilder summaryBuilder,
                             CatalogDefRepository catalog,
                             ObjectMapper objectMapper,
                             SchoolRules schoolRules,
                             ProjectRules projectRules,
                             RecruitmentRules recruitmentRules) {
        this.playerRepository = playerRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.commandExecutor = commandExecutor;
        this.graduationPolicy = graduationPolicy;
        this.summaryBuilder = summaryBuilder;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
        this.schoolRules = schoolRules;
        this.projectRules = projectRules;
        this.recruitmentRules = recruitmentRules;
    }

    @Override
    public List<ApplicationView> getApplications(UUID playerId) {
        playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        return jobApplicationRepository.findByPlayerIdOrderByCompanyCodeAsc(playerId).stream()
                .map(CareerServiceImpl::toView)
                .toList();
    }

    @Override
    @Transactional
    public ApplicationMutation apply(UUID playerId, long expectedVersion, String idempotencyKey,
                                     ApplyJobRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        JobApplication[] saved = new JobApplication[1];
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("JOB_APPLY", request.companyCode(), request, ApplicationMutation.class),
                player -> {
                    schoolRules.requireSchool(player);
                    schoolRules.requireNoPendingChoice(player);
                    CompanyRule rule = recruitmentRules.find(player, request.companyCode());
                    requireApplicable(player, rule);
                    spend(player, STEP_AP);

                    int portfolio = projectRules.portfolioScore(player, rule.role());
                    int document = recruitmentRules.documentScore(player, portfolio);
                    boolean passed = document >= rule.docPass();
                    saved[0] = jobApplicationRepository.save(JobApplication.apply(player.getId(), rule.code(),
                            rule.role(), schoolRules.absoluteTurn(player), portfolio, document, passed));
                    details.put("companyCode", rule.code());
                    details.put("score", document);
                    details.put("passScore", rule.docPass());
                },
                player -> new ApplicationMutation(toView(saved[0]), new MutationResult(player.view(), "JOB_APPLY",
                        STEP_AP, Map.of(), saved[0].getStatus() == ApplicationStatus.REJECTED ? "REJECTED" : "DOC_PASSED",
                        details)));
    }

    @Override
    @Transactional
    public ApplicationMutation proceedStep(UUID playerId, UUID applicationId, long expectedVersion,
                                           String idempotencyKey, JobStepRequest request) {
        String step = parseStep(request.step());
        Map<String, Object> details = new LinkedHashMap<>();
        JobApplication[] target = new JobApplication[1];
        int[] spent = new int[1];
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("JOB_STEP", applicationId + ":" + step, request, ApplicationMutation.class),
                player -> {
                    schoolRules.requireSchool(player);
                    schoolRules.requireNoPendingChoice(player);
                    JobApplication application = jobApplicationRepository.findByIdAndPlayerId(applicationId, player.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "지원 내역을 찾을 수 없습니다."));
                    target[0] = application;
                    CompanyRule rule = recruitmentRules.find(player, application.getCompanyCode());
                    requireStepOrder(application, step);
                    if (schoolRules.absoluteTurn(player) > rule.deadlineTurn()) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "채용 마감이 지났습니다.",
                                Map.of("deadlineTurn", rule.deadlineTurn()));
                    }
                    details.put("companyCode", rule.code());
                    details.put("step", step);
                    switch (step) {
                        case RecruitmentRules.STEP_CODING -> {
                            spend(player, STEP_AP);
                            spent[0] = STEP_AP;
                            int score = recruitmentRules.codingScore(player, rule);
                            application.recordCoding(score, score >= rule.codingPass());
                            details.put("score", score);
                            details.put("passScore", rule.codingPass());
                        }
                        case RecruitmentRules.STEP_INTERVIEW -> {
                            spend(player, STEP_AP);
                            spent[0] = STEP_AP;
                            int score = recruitmentRules.interviewScore(player, rule);
                            application.recordInterview(score, score >= rule.interviewPass());
                            details.put("score", score);
                            details.put("passScore", rule.interviewPass());
                        }
                        default -> {
                            int composite = recruitmentRules.compositeScore(application.getD().intValue(),
                                    application.getK().intValue(), application.getJ().intValue());
                            application.recordResult(composite, composite >= rule.offerPass());
                            details.put("score", composite);
                            details.put("passScore", rule.offerPass());
                        }
                    }
                },
                player -> new ApplicationMutation(toView(target[0]), new MutationResult(player.view(), "JOB_STEP",
                        spent[0], Map.of(), target[0].getStatus().name(), details)));
    }

    @Override
    @Transactional
    public List<String> expireOverdue(Player player, int nextAbsoluteTurn) {
        List<String> expired = new ArrayList<>();
        for (JobApplication application : jobApplicationRepository.findByPlayerIdAndStatusIn(player.getId(), IN_PROGRESS)) {
            if (recruitmentRules.find(player, application.getCompanyCode()).deadlineTurn() < nextAbsoluteTurn) {
                application.expire();
                expired.add(application.getCompanyCode());
            }
        }
        return expired;
    }

    private void requireApplicable(Player player, CompanyRule rule) {
        List<String> reasons = new ArrayList<>();
        if (player.getYear() < rule.minYear()) {
            reasons.add(rule.minYear() + "학년부터 지원할 수 있습니다.");
        }
        if (schoolRules.absoluteTurn(player) > rule.deadlineTurn()) {
            reasons.add("채용 마감이 지났습니다. (마감 " + rule.deadlineTurn() + "턴)");
        }
        if (jobApplicationRepository.existsByPlayerIdAndCompanyCode(player.getId(), rule.code())) {
            reasons.add("이미 지원한 기업입니다.");
        }
        if (!reasons.isEmpty()) {
            throw new BusinessException(ErrorCode.LOCKED, "지원할 수 없습니다.", Map.of("reasons", reasons));
        }
    }

    /** 단계는 직전 단계를 통과한 상태에서만 진행할 수 있다. */
    private static void requireStepOrder(JobApplication application, String step) {
        ApplicationStatus required = switch (step) {
            case RecruitmentRules.STEP_CODING -> ApplicationStatus.DOC_PASSED;
            case RecruitmentRules.STEP_INTERVIEW -> ApplicationStatus.CODE_PASSED;
            default -> ApplicationStatus.INTERVIEW_PASSED;
        };
        if (application.getStatus() != required) {
            throw new BusinessException(ErrorCode.WRONG_STEP, "이전 전형을 통과해야 진행할 수 있습니다.",
                    Map.of("status", application.getStatus().name(), "required", required.name()));
        }
    }

    private static String parseStep(String raw) {
        String step = raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of(RecruitmentRules.STEP_CODING, RecruitmentRules.STEP_INTERVIEW,
                RecruitmentRules.STEP_RESULT).contains(step)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "알 수 없는 채용 단계입니다: " + raw,
                    Map.of("allowed", List.of(RecruitmentRules.STEP_CODING, RecruitmentRules.STEP_INTERVIEW,
                            RecruitmentRules.STEP_RESULT)));
        }
        return step;
    }

    private static ApplicationView toView(JobApplication application) {
        return new ApplicationView(application.getId(), application.getCompanyCode(), application.getRole(),
                application.getStatus().name(), application.getD(), application.getK(), application.getJ(),
                application.getP(), application.getFailureCode());
    }

    private static void spend(Player player, int cost) {
        if (player.getAp() < cost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_AP,
                    Map.of("required", cost, "remaining", player.getAp()));
        }
        player.spendAp(cost);
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

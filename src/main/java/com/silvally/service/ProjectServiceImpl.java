package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.ProjectView;
import com.silvally.entity.Player;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.ProjectStatus;
import com.silvally.repository.PlayerProjectRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {
    static final int WORK_AP = 1;

    private final PlayerRepository playerRepository;
    private final PlayerProjectRepository projectRepository;
    private final CommandExecutor commandExecutor;
    private final SchoolRules schoolRules;
    private final ProjectRules projectRules;

    public ProjectServiceImpl(PlayerRepository playerRepository,
                              PlayerProjectRepository projectRepository,
                              CommandExecutor commandExecutor,
                              SchoolRules schoolRules,
                              ProjectRules projectRules) {
        this.playerRepository = playerRepository;
        this.projectRepository = projectRepository;
        this.commandExecutor = commandExecutor;
        this.schoolRules = schoolRules;
        this.projectRules = projectRules;
    }

    @Override
    public List<ProjectView> getProjects(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        return projectRules.all(player).stream().map(rule -> toView(player, rule)).toList();
    }

    @Override
    @Transactional
    public MutationResult start(UUID playerId, String projectCode, long expectedVersion, String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("PROJECT_START", projectCode, Map.of(), MutationResult.class),
                player -> {
                    ProjectRule rule = startCommand(player, projectCode);
                    List<String> reasons = startBlockReasons(player, rule);
                    if (!reasons.isEmpty()) {
                        throw new BusinessException(ErrorCode.LOCKED, "프로젝트를 시작할 수 없습니다.",
                                Map.of("reasons", reasons));
                    }
                    int startTurn = schoolRules.absoluteTurn(player);
                    int endTurn = startTurn + rule.durationTurns() - 1;
                    projectRepository.save(PlayerProject.start(player.getId(), projectCode, player.getYear(),
                            startTurn, endTurn));
                    details.put("projectCode", projectCode);
                    details.put("startTurn", startTurn);
                    details.put("endTurn", endTurn);
                },
                player -> new MutationResult(player.view(), "PROJECT_START", 0, Map.of(), "STARTED", details));
    }

    @Override
    @Transactional
    public MutationResult work(UUID playerId, String projectCode, long expectedVersion, String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        String[] result = new String[1];
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("PROJECT_WORK", projectCode, Map.of(), MutationResult.class),
                player -> {
                    ProjectRule rule = startCommand(player, projectCode);
                    PlayerProject project = activeProject(player, projectCode);
                    int turn = schoolRules.absoluteTurn(player);
                    if (turn > project.getEndTurn()) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "프로젝트 기간이 끝났습니다.");
                    }
                    List<ProjectRules.Snapshot> snapshots = new ArrayList<>(projectRules.snapshots(project));
                    if (snapshots.stream().anyMatch(s -> s.turn() == turn)) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "같은 턴에는 한 번만 작업할 수 있습니다.");
                    }
                    spend(player, WORK_AP);
                    snapshots.add(new ProjectRules.Snapshot(turn, projectRules.snapshotScore(player, rule)));
                    project.saveSnapshots(projectRules.toJson(snapshots));
                    details.put("projectCode", projectCode);
                    details.put("workCount", snapshots.size());
                    details.put("workRequired", rule.workRequired());
                    result[0] = "WORKED";
                    if (snapshots.size() >= rule.workRequired()) {
                        int quality = projectRules.quality(snapshots.stream().map(ProjectRules.Snapshot::score).toList());
                        boolean passed = quality >= rule.passQuality();
                        project.finish(quality, passed);
                        details.put("quality", quality);
                        details.put("passQuality", rule.passQuality());
                        result[0] = passed ? "COMPLETE" : "FAILED";
                    }
                },
                player -> new MutationResult(player.view(), "PROJECT_WORK", WORK_AP, Map.of(), result[0], details));
    }

    @Override
    @Transactional
    public MutationResult registerPortfolio(UUID playerId, String projectCode, long expectedVersion,
                                            String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("PORTFOLIO_REGISTER", projectCode, Map.of(), MutationResult.class),
                player -> {
                    ProjectRule rule = startCommand(player, projectCode);
                    PlayerProject project = projectRepository.findByPlayerIdAndProjectCode(player.getId(), projectCode)
                            .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_STEP, "시작하지 않은 프로젝트입니다."));
                    if (project.getStatus() != ProjectStatus.COMPLETE) {
                        throw new BusinessException(ErrorCode.LOCKED, "완료한 작품만 포트폴리오에 등록할 수 있습니다.",
                                Map.of("status", project.getStatus().name()));
                    }
                    if (project.isInPortfolio()) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "이미 포트폴리오에 등록된 작품입니다.");
                    }
                    if (projectRules.portfolio(player.getId()).size() >= ProjectRules.PORTFOLIO_SLOTS) {
                        throw new BusinessException(ErrorCode.LOCKED, "포트폴리오는 작품 "
                                + ProjectRules.PORTFOLIO_SLOTS + "개까지 등록할 수 있습니다.");
                    }
                    project.registerPortfolio();
                    details.put("projectCode", projectCode);
                    details.put("portfolioCount", projectRules.portfolio(player.getId()).size());
                    details.put("portfolioScore", projectRules.portfolioScore(player, rule.role()));
                    details.put("role", rule.role());
                },
                player -> new MutationResult(player.view(), "PORTFOLIO_REGISTER", 0, Map.of(), "REGISTERED", details));
    }

    @Override
    @Transactional
    public List<String> expireOverdue(Player player, int nextAbsoluteTurn) {
        List<String> expired = new ArrayList<>();
        for (PlayerProject project : projectRepository.findByPlayerId(player.getId())) {
            if (project.getStatus() == ProjectStatus.IN_PROGRESS && project.getEndTurn() < nextAbsoluteTurn) {
                project.expire();
                expired.add(project.getProjectCode());
            }
        }
        return expired;
    }

    private ProjectRule startCommand(Player player, String projectCode) {
        schoolRules.requireSchool(player);
        schoolRules.requireNoPendingChoice(player);
        return projectRules.find(player, projectCode);
    }

    private PlayerProject activeProject(Player player, String projectCode) {
        return projectRepository.findByPlayerIdAndProjectCode(player.getId(), projectCode)
                .filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_STEP, "진행 중인 프로젝트가 아닙니다."));
    }

    private List<String> startBlockReasons(Player player, ProjectRule rule) {
        List<String> reasons = new ArrayList<>();
        if (player.getYear() < rule.minYear()) {
            reasons.add(rule.minYear() + "학년부터 시작할 수 있습니다.");
        }
        if (projectRepository.findByPlayerIdAndProjectCode(player.getId(), rule.code()).isPresent()) {
            reasons.add("이미 진행했거나 끝낸 프로젝트입니다.");
        }
        boolean otherRunning = projectRepository.findByPlayerId(player.getId()).stream()
                .anyMatch(project -> project.getStatus() == ProjectStatus.IN_PROGRESS);
        if (otherRunning) {
            reasons.add("진행 중인 프로젝트를 먼저 마쳐야 합니다.");
        }
        return reasons;
    }

    private ProjectView toView(Player player, ProjectRule rule) {
        Optional<PlayerProject> current = projectRepository.findByPlayerIdAndProjectCode(player.getId(), rule.code());
        List<String> reasons = startBlockReasons(player, rule);
        return new ProjectView(rule.code(), rule.title(), rule.role(), rule.minYear(), rule.durationTurns(),
                rule.workRequired(), rule.passQuality(),
                current.map(p -> p.getStatus().name()).orElse(null),
                current.map(PlayerProject::getStartTurn).orElse(null),
                current.map(PlayerProject::getEndTurn).orElse(null),
                current.map(p -> projectRules.snapshots(p).size()).orElse(0),
                current.map(PlayerProject::getQuality).orElse(null),
                current.map(PlayerProject::isInPortfolio).orElse(false),
                reasons.isEmpty(), reasons);
    }

    private static void spend(Player player, int cost) {
        if (player.getAp() < cost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_AP,
                    Map.of("required", cost, "remaining", player.getAp()));
        }
        player.spendAp(cost);
    }
}

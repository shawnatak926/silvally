package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.Player;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.ProjectStatus;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 프로젝트 완성도와 포트폴리오 점수 규칙.
 *
 * <ul>
 *     <li>작업 스냅샷 점수 = 작업 시점 집중 능력치 평균, 완성도 = 스냅샷 평균 × 1.2 + 20 (0~100)</li>
 *     <li>포트폴리오는 완료 작품 {@value #PORTFOLIO_SLOTS}개까지 등록할 수 있다.</li>
 *     <li>직무 환산 점수 = 등록 작품의 (완성도 × 직무 가중치) 합 ÷ {@value #PORTFOLIO_DIVISOR},
 *     최대 {@value #PORTFOLIO_MAX}. 직무가 같은 작품은 1.0, 다른 작품은 0.5 가중치.</li>
 * </ul>
 */
@Component
public class ProjectRules {
    public static final int PORTFOLIO_SLOTS = 3;
    public static final int PORTFOLIO_MAX = 100;
    public static final double PORTFOLIO_DIVISOR = 2.0;
    static final double ROLE_MATCH_WEIGHT = 1.0;
    static final double ROLE_OTHER_WEIGHT = 0.5;

    private final CatalogDefRepository catalog;
    private final PlayerProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public ProjectRules(CatalogDefRepository catalog, PlayerProjectRepository projectRepository,
                        ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    public List<ProjectRule> all(Player player) {
        return catalog.findByKind(player.getRulesVersion(), CatalogKind.PROJECT.name()).stream()
                .map(this::toRule)
                .toList();
    }

    public ProjectRule find(Player player, String projectCode) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), CatalogKind.PROJECT.name(), projectCode))
                .map(this::toRule)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다: " + projectCode));
    }

    /** 작업 시점의 집중 능력치 평균. */
    public int snapshotScore(Player player, ProjectRule rule) {
        Map<String, Integer> stats = player.getStats().toMap();
        double mean = rule.focusStats().stream().mapToInt(stat -> stats.getOrDefault(stat, 0)).average().orElse(0);
        return (int) Math.round(mean);
    }

    public int quality(List<Integer> snapshotScores) {
        double mean = snapshotScores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return clamp((int) Math.round(mean * 1.2 + 20));
    }

    public List<Snapshot> snapshots(PlayerProject project) {
        List<Snapshot> result = new ArrayList<>();
        String json = project.getWorkSnapshotsJson();
        if (json == null || json.isBlank()) {
            return result;
        }
        objectMapper.readTree(json).forEach(node ->
                result.add(new Snapshot(node.path("turn").asInt(), node.path("score").asInt())));
        return result;
    }

    public String toJson(List<Snapshot> snapshots) {
        return objectMapper.writeValueAsString(snapshots);
    }

    /** 포트폴리오에 등록된 완료 작품. */
    public List<PlayerProject> portfolio(UUID playerId) {
        return projectRepository.findByPlayerId(playerId).stream()
                .filter(project -> project.getStatus() == ProjectStatus.COMPLETE && project.isInPortfolio())
                .toList();
    }

    /** 지정 직무 기준 포트폴리오 환산 점수(0~{@value #PORTFOLIO_MAX}). */
    public int portfolioScore(Player player, String role) {
        double sum = portfolio(player.getId()).stream()
                .sorted(Comparator.comparing(PlayerProject::getProjectCode))
                .mapToDouble(project -> project.getQuality() * roleWeight(player, project, role))
                .sum();
        return Math.min(PORTFOLIO_MAX, (int) Math.round(sum / PORTFOLIO_DIVISOR));
    }

    private double roleWeight(Player player, PlayerProject project, String role) {
        String projectRole = find(player, project.getProjectCode()).role();
        return projectRole.equals(role) ? ROLE_MATCH_WEIGHT : ROLE_OTHER_WEIGHT;
    }

    private ProjectRule toRule(CatalogDef def) {
        JsonNode config = objectMapper.readTree(def.getConfigJson());
        List<String> focus = new ArrayList<>();
        config.path("focus").forEach(node -> focus.add(node.asString()));
        return new ProjectRule(def.getId().code(), def.getTitle(), config.path("role").asString(),
                config.path("minYear").asInt(1), config.path("durationTurns").asInt(4),
                config.path("workRequired").asInt(2), focus, config.path("passQuality").asInt(45));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record Snapshot(int turn, int score) {
    }
}

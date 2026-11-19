package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.CourseStatus;
import com.silvally.entity.Player;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * 채용 전형 점수 규칙. 상태를 바꾸지 않는다.
 *
 * <ul>
 *     <li>서류 = 포트폴리오 환산 점수 × 0.7 + 자격증(최대 {@value #CERTIFICATE_CAP}개) × {@value #CERTIFICATE_POINT} + 이력서 레벨</li>
 *     <li>코딩·회사 면접 = 기업이 정한 능력치 평균 × 1.2 + 20 + 기업·단계·회차별 고정 변동(±5)</li>
 *     <li>최종 종합 = 서류 × 0.3 + 코딩 × 0.35 + 면접 × 0.35</li>
 * </ul>
 */
@Component
public class RecruitmentRules {
    public static final String STEP_CODING = "CODING";
    public static final String STEP_INTERVIEW = "INTERVIEW";
    public static final String STEP_RESULT = "RESULT";

    static final int CERTIFICATE_CAP = 3;
    static final int CERTIFICATE_POINT = 5;

    private final CatalogDefRepository catalog;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    public RecruitmentRules(CatalogDefRepository catalog, CourseEnrollmentRepository enrollmentRepository,
                            ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.enrollmentRepository = enrollmentRepository;
        this.objectMapper = objectMapper;
    }

    public CompanyRule find(Player player, String companyCode) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), CatalogKind.COMPANY.name(), companyCode))
                .map(this::toRule)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기업을 찾을 수 없습니다: " + companyCode));
    }

    public int documentScore(Player player, int portfolioScore) {
        long certificates = enrollmentRepository.findByPlayerId(player.getId()).stream()
                .filter(e -> e.getStatus() == CourseStatus.PASSED)
                .count();
        // 0.7 같은 실수 곱셈은 .5 경계에서 오차로 반올림이 갈리므로 정수로 계산한다.
        double score = portfolioScore * 7 / 10.0 + Math.min(certificates, CERTIFICATE_CAP) * CERTIFICATE_POINT
                + player.getResumeLevel();
        return clamp((int) Math.round(score));
    }

    public int codingScore(Player player, CompanyRule rule) {
        return stepScore(player, rule, STEP_CODING, rule.codingStats());
    }

    public int interviewScore(Player player, CompanyRule rule) {
        return stepScore(player, rule, STEP_INTERVIEW, rule.interviewStats());
    }

    public int compositeScore(int document, int coding, int interview) {
        return clamp((int) Math.round((document * 30 + coding * 35 + interview * 35) / 100.0));
    }

    private int stepScore(Player player, CompanyRule rule, String step, List<String> statCodes) {
        Map<String, Integer> stats = player.getStats().toMap();
        double mean = statCodes.stream().mapToInt(stat -> stats.getOrDefault(stat, 0)).average().orElse(0);
        SplittableRandom random = new SplittableRandom((player.getSeed() + rule.code() + step).hashCode());
        return clamp((int) Math.round(mean * 1.2 + 20 + random.nextInt(-5, 6)));
    }

    private CompanyRule toRule(CatalogDef def) {
        JsonNode config = objectMapper.readTree(def.getConfigJson());
        return new CompanyRule(def.getId().code(), def.getTitle(), config.path("role").asString(),
                config.path("minYear").asInt(1), config.path("deadlineTurn").asInt(),
                config.path("docPass").asInt(), config.path("codingPass").asInt(),
                config.path("interviewPass").asInt(), config.path("offerPass").asInt(),
                strings(config.path("codingStats")), strings(config.path("interviewStats")));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asString()));
        return values;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}

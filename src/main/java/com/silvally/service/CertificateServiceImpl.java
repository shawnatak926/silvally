package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.CourseView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.CourseStatus;
import com.silvally.entity.Player;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CertificateServiceImpl implements CertificateService {
    static final int ATTEND_AP = 1;
    static final int EXAM_AP = 1;
    static final int REQUIRED_ATTENDANCE = 2;

    private final PlayerRepository playerRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CatalogDefRepository catalog;
    private final CommandExecutor commandExecutor;
    private final SchoolRules schoolRules;
    private final ObjectMapper objectMapper;

    public CertificateServiceImpl(PlayerRepository playerRepository,
                                  CourseEnrollmentRepository enrollmentRepository,
                                  CatalogDefRepository catalog,
                                  CommandExecutor commandExecutor,
                                  SchoolRules schoolRules,
                                  ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.catalog = catalog;
        this.commandExecutor = commandExecutor;
        this.schoolRules = schoolRules;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CourseView> getCourses(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        return catalog.findByKind(player.getRulesVersion(), CatalogKind.COURSE.name()).stream()
                .map(this::toRule)
                .map(rule -> {
                    Optional<CourseEnrollment> current = currentEnrollment(player, rule.code());
                    List<String> reasons = enrollBlockReasons(player, rule);
                    return new CourseView(rule.code(), rule.title(), rule.certificate(), rule.minYear(),
                            rule.requires(), rule.passScore(), rule.validYears(),
                            current.map(e -> e.getStatus().name()).orElse(null),
                            current.map(CourseEnrollment::attendanceCount).orElse(0),
                            reasons.isEmpty(), reasons);
                })
                .toList();
    }

    @Override
    @Transactional
    public MutationResult enroll(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("COURSE_ENROLL", courseCode, Map.of(), MutationResult.class),
                player -> {
                    CourseRule rule = startCommand(player, courseCode);
                    List<String> reasons = enrollBlockReasons(player, rule);
                    if (!reasons.isEmpty()) {
                        throw new BusinessException(ErrorCode.LOCKED, "수강 조건을 만족하지 않습니다.",
                                Map.of("reasons", reasons));
                    }
                    enrollmentRepository.save(CourseEnrollment.enroll(player.getId(), courseCode, player.getYear()));
                    details.put("courseCode", courseCode);
                    details.put("certificate", rule.certificate());
                },
                player -> new MutationResult(player.view(), "COURSE_ENROLL", 0, Map.of(), "ENROLLED", details));
    }

    @Override
    @Transactional
    public MutationResult attend(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("COURSE_ATTEND", courseCode, Map.of(), MutationResult.class),
                player -> {
                    startCommand(player, courseCode);
                    CourseEnrollment enrollment = activeEnrollment(player, courseCode);
                    if (enrollment.attendanceCount() >= REQUIRED_ATTENDANCE) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "출석을 이미 2회 마쳤습니다. 시험을 응시하세요.");
                    }
                    if (enrollment.attendedAt(player.getTurn())) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "같은 턴에는 한 번만 출석할 수 있습니다.");
                    }
                    spend(player, ATTEND_AP);
                    enrollment.attend(player.getTurn());
                    details.put("courseCode", courseCode);
                    details.put("attendanceCount", enrollment.attendanceCount());
                },
                player -> new MutationResult(player.view(), "COURSE_ATTEND", ATTEND_AP, Map.of(), "ATTENDED", details));
    }

    @Override
    @Transactional
    public MutationResult takeExam(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey) {
        Map<String, Object> details = new LinkedHashMap<>();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("COURSE_EXAM", courseCode, Map.of(), MutationResult.class),
                player -> {
                    CourseRule rule = startCommand(player, courseCode);
                    CourseEnrollment enrollment = activeEnrollment(player, courseCode);
                    if (enrollment.attendanceCount() < REQUIRED_ATTENDANCE) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "시험 응시 전 출석 2회가 필요합니다.",
                                Map.of("attendanceCount", enrollment.attendanceCount()));
                    }
                    spend(player, EXAM_AP);
                    int score = examScore(player, rule);
                    boolean passed = score >= rule.passScore();
                    enrollment.recordExam(BigDecimal.valueOf(score), passed, player.getTurn());
                    details.put("courseCode", courseCode);
                    details.put("score", score);
                    details.put("passScore", rule.passScore());
                    details.put("result", passed ? "PASSED" : "FAILED");
                },
                player -> new MutationResult(player.view(), "COURSE_EXAM", EXAM_AP, Map.of(),
                        String.valueOf(details.get("result")), details));
    }

    @Override
    @Transactional
    public List<String> expireAtYearEnd(Player player, int nextYear) {
        List<String> expired = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollmentRepository.findByPlayerId(player.getId())) {
            boolean unfinished = enrollment.getStatus() == CourseStatus.ENROLLED
                    && enrollment.getEnrolledYear() < nextYear;
            boolean outdated = enrollment.getStatus() == CourseStatus.PASSED
                    && validityEnded(player, enrollment, nextYear);
            if (unfinished || outdated) {
                enrollment.expire();
                expired.add(enrollment.getCourseCode());
            }
        }
        return expired;
    }

    int examScore(Player player, CourseRule rule) {
        Map<String, Integer> stats = player.getStats().toMap();
        double mean = rule.examStats().stream().mapToInt(stats::get).average().orElse(0);
        SplittableRandom random = new SplittableRandom(
                (player.getSeed() + rule.code() + player.getYear()).hashCode());
        int score = (int) Math.round(mean * 1.2 + 20 + random.nextInt(-5, 6));
        return Math.max(0, Math.min(100, score));
    }

    private CourseRule startCommand(Player player, String courseCode) {
        schoolRules.requireSchool(player);
        schoolRules.requireNoPendingChoice(player);
        return findRule(player, courseCode);
    }

    private List<String> enrollBlockReasons(Player player, CourseRule rule) {
        List<String> reasons = new ArrayList<>();
        if (player.getYear() < rule.minYear()) {
            reasons.add(rule.minYear() + "학년부터 수강할 수 있습니다.");
        }
        Map<String, Integer> stats = player.getStats().toMap();
        rule.requires().forEach((stat, required) -> {
            if (stats.getOrDefault(stat, 0) < required) {
                reasons.add(stat + " " + required + " 이상이 필요합니다. (현재 " + stats.get(stat) + ")");
            }
        });
        if (currentEnrollment(player, rule.code()).isPresent()) {
            reasons.add("이번 학년에 이미 수강 신청했습니다.");
        }
        boolean holding = enrollmentRepository.findByPlayerIdAndCourseCode(player.getId(), rule.code()).stream()
                .anyMatch(e -> e.getStatus() == CourseStatus.PASSED);
        if (holding) {
            reasons.add("이미 취득한 자격증입니다.");
        }
        return reasons;
    }

    private Optional<CourseEnrollment> currentEnrollment(Player player, String courseCode) {
        return enrollmentRepository.findByPlayerIdAndCourseCode(player.getId(), courseCode).stream()
                .filter(e -> e.getEnrolledYear() == player.getYear())
                .findFirst();
    }

    private CourseEnrollment activeEnrollment(Player player, String courseCode) {
        return currentEnrollment(player, courseCode)
                .filter(e -> e.getStatus() == CourseStatus.ENROLLED)
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_STEP, "수강 중인 과정이 아닙니다."));
    }

    private boolean validityEnded(Player player, CourseEnrollment enrollment, int nextYear) {
        CourseRule rule = findRule(player, enrollment.getCourseCode());
        return rule.validYears() > 0 && nextYear > enrollment.getEnrolledYear() + rule.validYears();
    }

    private static void spend(Player player, int cost) {
        if (player.getAp() < cost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_AP,
                    Map.of("required", cost, "remaining", player.getAp()));
        }
        player.spendAp(cost);
    }

    private CourseRule findRule(Player player, String courseCode) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), CatalogKind.COURSE.name(), courseCode))
                .map(this::toRule)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "방과후 과정을 찾을 수 없습니다: " + courseCode));
    }

    private CourseRule toRule(CatalogDef def) {
        JsonNode config = objectMapper.readTree(def.getConfigJson());
        Map<String, Integer> requires = new LinkedHashMap<>();
        config.path("requires").properties().forEach(e -> requires.put(e.getKey(), e.getValue().asInt()));
        List<String> examStats = new ArrayList<>();
        config.path("exam").forEach(node -> examStats.add(node.asString()));
        return new CourseRule(def.getId().code(), def.getTitle(), config.path("certificate").asString(),
                config.path("minYear").asInt(1), requires, examStats,
                config.path("passScore").asInt(), config.path("validYears").asInt(0));
    }
}

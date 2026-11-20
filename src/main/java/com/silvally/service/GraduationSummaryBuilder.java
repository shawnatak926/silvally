package com.silvally.service;

import com.silvally.entity.ActivityRecord;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.CourseStatus;
import com.silvally.entity.JobApplication;
import com.silvally.entity.Player;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.ProjectStatus;
import com.silvally.repository.ActivityRecordRepository;
import com.silvally.repository.CatalogDefRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 졸업 시점의 진로·자격증·프로젝트·현장학습 결과를 한 장의 요약으로 모은다.
 * 요약은 엔딩과 함께 저장되어 이후 상태가 바뀌어도 졸업 당시 내용이 유지된다.
 */
@Component
public class GraduationSummaryBuilder {

    /** 현장실습·현장학습 결과를 남기는 활동 이벤트 코드의 접두사. */
    public static final String FIELD_EVENT_PREFIX = "FIELD_";

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final PlayerProjectRepository playerProjectRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final CatalogDefRepository catalog;
    private final ObjectMapper objectMapper;
    private final ExamService exams;
    private final NarshaService narsha;

    public GraduationSummaryBuilder(ExamService exams, CourseEnrollmentRepository courseEnrollmentRepository,
                                    PlayerProjectRepository playerProjectRepository,
                                    ActivityRecordRepository activityRecordRepository,
                                    CatalogDefRepository catalog,
                                    ObjectMapper objectMapper, NarshaService narsha) {
        this.narsha = narsha;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.playerProjectRepository = playerProjectRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
        this.exams = exams;
    }

    /** 포트폴리오에 등록된 완료 작품 수. */
    public int portfolioCount(UUID playerId) {
        return (int) playerProjectRepository.findByPlayerId(playerId).stream()
                .filter(GraduationSummaryBuilder::inPortfolio)
                .count();
    }

    public Map<String, Object> build(Player player, GraduationPolicy.Decision decision, GraduationPath requested) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requestedPath", requested.name());
        summary.put("path", decision.path().name());
        summary.put("initialCareerCode", player.getInitialCareerCode());
        summary.put("currentCareerCode", player.getCurrentCareerCode());
        summary.put("careerChanged", careerChanged(player));
        summary.put("aptitudeMismatch", decision.aptitudeMismatch());
        summary.put("year", player.getYear());
        summary.put("turn", player.getTurn());
        summary.put("major", player.getMajor());
        summary.put("track", player.getTrack());
        summary.put("stats", player.getStats().toMap());
        summary.put("grade", player.getStats().getG());
        summary.put("exams", exams.results(player.getId()).stream()
                .map(exam -> objectMapper.convertValue(exam, Map.class)).toList());
        summary.put("certificates", certificates(player));
        summary.put("projects", projects(player.getId()));
        summary.put("portfolioCount", portfolioCount(player.getId()));
        // 1·2학년 나르샤는 별도 기록으로 졸업 당시 주제·완성도·등위·전시 여부를 그대로 남긴다.
        summary.put("narshaProjects", narsha.projects(player.getId()));
        summary.put("fieldTrips", fieldTrips(player.getId()));
        JobApplication accepted = decision.accepted();
        if (accepted != null) {
            // 저장 후 JSON에서 복원해도 같은 값이 되도록 UUID를 문자열로 남긴다.
            summary.put("offer", Map.of("applicationId", accepted.getId().toString(),
                    "companyCode", accepted.getCompanyCode(), "role", accepted.getRole()));
        }
        return summary;
    }

    private static boolean careerChanged(Player player) {
        String initial = player.getInitialCareerCode();
        String current = player.getCurrentCareerCode();
        return initial != null && current != null && !initial.equals(current);
    }

    private List<Map<String, Object>> certificates(Player player) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseEnrollment enrollment : courseEnrollmentRepository.findByPlayerId(player.getId())) {
            if (enrollment.getStatus() != CourseStatus.PASSED) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseCode", enrollment.getCourseCode());
            item.put("certificate", certificateName(player, enrollment.getCourseCode()));
            item.put("earnedYear", enrollment.getEnrolledYear());
            item.put("earnedAtTurn", enrollment.getEarnedAtTurn());
            result.add(item);
        }
        return result;
    }

    private String certificateName(Player player, String courseCode) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), CatalogKind.COURSE.name(), courseCode))
                .map(def -> objectMapper.readTree(def.getConfigJson()).path("certificate").asString(def.getTitle()))
                .orElse(courseCode);
    }

    private List<Map<String, Object>> projects(UUID playerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayerProject project : playerProjectRepository.findByPlayerId(playerId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("projectCode", project.getProjectCode());
            item.put("status", project.getStatus().name());
            item.put("quality", project.getQuality());
            item.put("inPortfolio", project.isInPortfolio());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> fieldTrips(UUID playerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityRecord record : activityRecordRepository.findByPlayerIdOrderByIdAsc(playerId)) {
            boolean currentProgram = record.getEventCode().equals("J01") || record.getEventCode().startsWith("J01_");
            if (!record.getEventCode().startsWith(FIELD_EVENT_PREFIX) && !currentProgram) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventCode", record.getEventCode());
            item.put("choiceCode", record.getChoiceCode());
            item.put("resultCode", record.getResultCode());
            if (currentProgram && record.getEvidenceJson() != null && !record.getEvidenceJson().isBlank()) {
                item.put("evidence", objectMapper.readValue(record.getEvidenceJson(), Map.class));
            }
            result.add(item);
        }
        return result;
    }

    private static boolean inPortfolio(PlayerProject project) {
        return project.getStatus() == ProjectStatus.COMPLETE && project.isInPortfolio();
    }
}

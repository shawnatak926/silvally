package com.silvally.service;

import com.silvally.dto.response.ExamResultView;
import com.silvally.dto.response.ProgressView;
import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.CourseStatus;
import com.silvally.entity.Player;
import com.silvally.entity.PlayerProject;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 저장된 수강·프로젝트·시험 기록을 진행 현황 화면 한 장으로 모은다. */
@Component
public class ProgressAssembler {

    private final CourseEnrollmentRepository enrollments;
    private final PlayerProjectRepository projects;
    private final ProjectRules projectRules;
    private final ExamService exams;
    private final NarshaService narsha;

    public ProgressAssembler(CourseEnrollmentRepository enrollments, PlayerProjectRepository projects,
                             ProjectRules projectRules, ExamService exams, NarshaService narsha) {
        this.narsha = narsha;
        this.enrollments = enrollments;
        this.projects = projects;
        this.projectRules = projectRules;
        this.exams = exams;
    }

    public ProgressView assemble(Player player) {
        List<CourseEnrollment> courses = enrollments.findByPlayerId(player.getId()).stream()
                .sorted(Comparator.comparing(CourseEnrollment::getEnrolledYear).thenComparing(CourseEnrollment::getCourseCode))
                .toList();
        List<PlayerProject> works = projects.findByPlayerId(player.getId()).stream()
                .sorted(Comparator.comparing(PlayerProject::getYear).thenComparing(PlayerProject::getStartTurn))
                .toList();
        List<ExamResultView> examResults = exams.results(player.getId());
        return new ProgressView(player.view(),
                courses.stream().map(ProgressAssembler::enrollment).toList(),
                works.stream().map(ProgressAssembler::project).toList(),
                courses.stream().filter(c -> c.getStatus() == CourseStatus.PASSED).map(ProgressAssembler::qualification).toList(),
                yearSummaries(player, examResults),
                snapshots(works),
                narsha.projects(player.getId()));
    }

    private static Map<String, Object> enrollment(CourseEnrollment course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("courseCode", course.getCourseCode());
        row.put("year", course.getEnrolledYear());
        row.put("status", course.getStatus().name());
        row.put("attendance", course.attendanceCount());
        row.put("examScore", course.getExamScore());
        return row;
    }

    private static Map<String, Object> qualification(CourseEnrollment course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("courseCode", course.getCourseCode());
        row.put("year", course.getEnrolledYear());
        row.put("earnedAtTurn", course.getEarnedAtTurn());
        return row;
    }

    private static Map<String, Object> project(PlayerProject project) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectCode", project.getProjectCode());
        row.put("year", project.getYear());
        row.put("status", project.getStatus().name());
        row.put("startTurn", project.getStartTurn());
        row.put("endTurn", project.getEndTurn());
        row.put("quality", project.getQuality());
        row.put("inPortfolio", project.isInPortfolio());
        return row;
    }

    private static List<Map<String, Object>> yearSummaries(Player player, List<ExamResultView> examResults) {
        int lastYear = Math.max(player.getYear(), examResults.stream().mapToInt(ExamResultView::year).max().orElse(0));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int year = 1; year <= lastYear; year++) {
            int y = year;
            List<ExamResultView> yearExams = examResults.stream().filter(e -> e.year() == y).toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("year", year);
            row.put("exams", yearExams.stream().map(ProgressAssembler::exam).toList());
            row.put("gradeAfter", yearExams.isEmpty() ? null : yearExams.getLast().gradeAfter());
            rows.add(row);
        }
        return rows;
    }

    private static Map<String, Object> exam(ExamResultView exam) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("semester", exam.semester());
        row.put("kind", exam.kind());
        row.put("turn", exam.turn());
        row.put("score", exam.score());
        row.put("gradeBefore", exam.gradeBefore());
        row.put("gradeAfter", exam.gradeAfter());
        row.put("ruleSource", exam.ruleSource());
        return row;
    }

    private List<Map<String, Object>> snapshots(List<PlayerProject> works) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlayerProject project : works) {
            for (ProjectRules.Snapshot snapshot : projectRules.snapshots(project)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("projectCode", project.getProjectCode());
                row.put("turn", snapshot.turn());
                row.put("score", snapshot.score());
                rows.add(row);
            }
        }
        return rows;
    }
}

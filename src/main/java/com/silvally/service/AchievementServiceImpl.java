package com.silvally.service;

import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.PlayerProject;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AchievementServiceImpl implements AchievementService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final PlayerProjectRepository playerProjectRepository;
    private final ProjectRules projectRules;

    public AchievementServiceImpl(CourseEnrollmentRepository courseEnrollmentRepository,
                                  PlayerProjectRepository playerProjectRepository,
                                  ProjectRules projectRules) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.playerProjectRepository = playerProjectRepository;
        this.projectRules = projectRules;
    }

    @Override
    public List<Map<String, Object>> getEnrollments(UUID playerId) {
        return courseEnrollmentRepository.findByPlayerId(playerId).stream()
                .sorted(Comparator.comparing(CourseEnrollment::getEnrolledYear)
                        .thenComparing(CourseEnrollment::getCourseCode))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseCode", e.getCourseCode());
                    row.put("year", e.getEnrolledYear());
                    row.put("status", e.getStatus().name());
                    row.put("attendanceCount", e.attendanceCount());
                    row.put("examScore", e.getExamScore());
                    row.put("earnedAtTurn", e.getEarnedAtTurn());
                    return row;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> getProjects(UUID playerId) {
        return playerProjectRepository.findByPlayerId(playerId).stream()
                .sorted(Comparator.comparing(PlayerProject::getStartTurn)
                        .thenComparing(PlayerProject::getProjectCode))
                .map(project -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("projectCode", project.getProjectCode());
                    row.put("year", project.getYear());
                    row.put("status", project.getStatus().name());
                    row.put("startTurn", project.getStartTurn());
                    row.put("endTurn", project.getEndTurn());
                    row.put("workCount", projectRules.snapshots(project).size());
                    row.put("quality", project.getQuality());
                    row.put("inPortfolio", project.isInPortfolio());
                    return row;
                })
                .toList();
    }
}

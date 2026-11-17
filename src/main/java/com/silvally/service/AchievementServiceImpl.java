package com.silvally.service;

import com.silvally.entity.CourseEnrollment;
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

    public AchievementServiceImpl(CourseEnrollmentRepository courseEnrollmentRepository,
                                  PlayerProjectRepository playerProjectRepository) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.playerProjectRepository = playerProjectRepository;
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
        throw new UnsupportedOperationException("TODO: 프로젝트 현황");
    }
}

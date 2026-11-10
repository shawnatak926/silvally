package com.silvally.service;

import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        throw new UnsupportedOperationException("TODO: 수강 현황");
    }

    @Override
    public List<Map<String, Object>> getProjects(UUID playerId) {
        throw new UnsupportedOperationException("TODO: 프로젝트 현황");
    }
}

package com.silvally.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AchievementService {
    List<Map<String, Object>> getEnrollments(UUID playerId);

    List<Map<String, Object>> getProjects(UUID playerId);
}

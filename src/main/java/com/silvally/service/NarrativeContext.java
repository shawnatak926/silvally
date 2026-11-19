package com.silvally.service;

import java.util.List;
import java.util.Map;

public record NarrativeContext(
        String playerName,
        String phase,
        int year,
        int turn,
        Integer beforeSchoolDay,
        Map<String, Integer> stats,
        String admissionTrack,
        String major,
        String track,
        String careerCode,
        List<Project> projects,
        List<String> certificates,
        List<RecentChoice> recentChoices
) {
    public record Project(String code, int year, String status, Integer quality) {
    }

    public record RecentChoice(int year, int turn, Integer beforeSchoolDay, String sceneId,
                               String choiceCode, String resultCode) {
    }
}

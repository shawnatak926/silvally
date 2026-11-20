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
        List<RecentChoice> recentChoices,
        List<SchoolEvidence> schoolEvidence
) {
    public record Project(String code, int year, String status, Integer quality) {
    }

    public record RecentChoice(int year, int turn, Integer beforeSchoolDay, String sceneId,
                               String choiceCode, String resultCode) {
    }

    /** 학교 자료 근거. classification과 gameAdaptation은 공통 근거 카탈로그 값을 그대로 쓴다. */
    public record SchoolEvidence(String code, String title, String url, Integer sourceYear, String appliesTo,
                                 String classification, String fact, String gameAdaptation) {
    }
}

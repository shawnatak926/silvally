package com.silvally.dto.response;

import java.util.Map;
import java.util.UUID;

public record PlayerView(
        UUID id,
        String name,
        String rulesVersion,
        String phase,
        int year,
        int turn,
        int remainingAp,
        long version,
        Map<String, Integer> stats,
        String applicationProfile,
        String admissionTrack,
        Integer beforeSchoolDay,
        String initialCareerCode,
        String currentCareerCode,
        String major,
        String track,
        int resumeScore,
        String edition
) {
    public PlayerView(UUID id, String name, String rulesVersion, String phase, int year, int turn, int remainingAp, long version,
            Map<String, Integer> stats, String applicationProfile, String admissionTrack, Integer beforeSchoolDay,
            String initialCareerCode, String currentCareerCode, String major, String track, int resumeScore) {
        this(id, name, rulesVersion, phase, year, turn, remainingAp, version, stats, applicationProfile, admissionTrack,
                beforeSchoolDay, initialCareerCode, currentCareerCode, major, track, resumeScore, "SPICY");
    }
}

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
        Map<String, Boolean> admissionEligibility,
        SchoolState school
) {
    public record SchoolState(
            String semesterFocus,
            boolean vacationPending,
            int practiceRemaining,
            String pendingChoice,
            String narshaStage,
            Integer narshaRank,
            boolean softwarewaveAttended,
            int examPreparation
    ) {
    }
}

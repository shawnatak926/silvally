package com.silvally.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CareerPreferenceView(
        String initialCareerCode,
        String currentCareerCode,
        List<Recommendation> recommendations,
        boolean canChange,
        List<String> eligibleChangeRoleCodes
) {

    public record Recommendation(
            int rank,
            String roleCode,
            String label,
            BigDecimal score,
            List<StatReason> statReasons,
            List<ActivityReason> activityReasons
    ) {
    }

    public record StatReason(String statCode, int value, BigDecimal contribution) {
    }

    public record ActivityReason(
            int day,
            String eventCode,
            String choiceCode,
            String activityLabel,
            String tag,
            BigDecimal contribution
    ) {
    }
}

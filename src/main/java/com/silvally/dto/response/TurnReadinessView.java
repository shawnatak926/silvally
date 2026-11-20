package com.silvally.dto.response;

import java.util.List;

public record TurnReadinessView(
        long version,
        String phase,
        int year,
        int turn,
        boolean canAdvance,
        List<Blocker> blockers
) {
    public record Blocker(String type, String code, List<String> options) {
    }
}

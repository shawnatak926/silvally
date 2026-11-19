package com.silvally.dto.response;

import java.util.List;

public record ProjectView(
        String code,
        String title,
        String role,
        int minYear,
        int durationTurns,
        int workRequired,
        int passQuality,
        String status,
        Integer startTurn,
        Integer endTurn,
        int workCount,
        Integer quality,
        boolean inPortfolio,
        boolean startable,
        List<String> reasons
) {
}

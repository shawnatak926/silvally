package com.silvally.dto.response;

import java.util.List;
import java.util.Map;

public record ProgressView(
        PlayerView player,
        List<Map<String, Object>> enrolments,
        List<Map<String, Object>> projects,
        List<Map<String, Object>> qualifications,
        List<Map<String, Object>> yearSummaries,
        List<Map<String, Object>> snapshots
) {
}

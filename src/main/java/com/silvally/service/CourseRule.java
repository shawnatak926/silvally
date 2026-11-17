package com.silvally.service;

import java.util.List;
import java.util.Map;

public record CourseRule(
        String code,
        String title,
        String certificate,
        int minYear,
        Map<String, Integer> requires,
        List<String> examStats,
        int passScore,
        int validYears
) {
}

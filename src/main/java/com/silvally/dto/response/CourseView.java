package com.silvally.dto.response;

import java.util.List;
import java.util.Map;

public record CourseView(
        String code,
        String title,
        String certificate,
        int minYear,
        Map<String, Integer> requires,
        int passScore,
        int validYears,
        String status,
        int attendanceCount,
        boolean enrollable,
        List<String> reasons
) {
}

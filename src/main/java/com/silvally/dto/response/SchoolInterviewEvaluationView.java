package com.silvally.dto.response;

import java.util.List;
import java.util.Map;

public record SchoolInterviewEvaluationView(int score, Map<String, Integer> components, String source,
                                            String rubricVersion, Map<String, Criterion> criteria) {
    public record Criterion(int score, int maxScore, List<String> evidence, List<String> reasons) { }
}

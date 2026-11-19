package com.silvally.service;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** AI 채점이 없는 환경의 공개된 기본 규칙. 항목별 0~25, 총 0~100점. */
@Component
public class SchoolInterviewEvaluator {
    public record Evaluation(int score, Map<String, Integer> components, String source) { }
    public Evaluation evaluate(String answer) {
        Map<String, Integer> parts = new LinkedHashMap<>();
        parts.put("motivation", score(answer, "지원", "동기", "관심"));
        parts.put("experience", score(answer, "활동", "프로젝트", "경험"));
        parts.put("problemSolving", score(answer, "문제", "해결", "오류"));
        parts.put("schoolPlan", score(answer, "입학", "계획", "학습"));
        return new Evaluation(parts.values().stream().mapToInt(Integer::intValue).sum(), parts, "RULE_BASED");
    }
    private int score(String answer, String... terms) {
        int matches = 0;
        for (String term : terms) if (answer.contains(term)) matches++;
        return matches == 0 ? 0 : Math.min(25, 10 + matches * 3 + Math.min(6, answer.length() / 40));
    }
}

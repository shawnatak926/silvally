package com.silvally.dto.response;

import java.util.List;
import java.util.Map;

/** 나르샤 화면용 조회: 제시 가능한 주제·역할, 입력 규칙, 학년별 프로젝트 기록. */
public record NarshaView(
        List<Topic> topics,
        List<Role> roles,
        Map<String, Object> customTopicRule,
        List<Map<String, Object>> projects,
        Map<String, Object> current,
        List<Map<String, String>> sources,
        String notice
) {
    public record Topic(String code, String title, String summary, List<String> features, List<String> roles) {
    }

    public record Role(String code, String label) {
    }
}

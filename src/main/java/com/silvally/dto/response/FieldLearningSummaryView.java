package com.silvally.dto.response;

import java.util.*;

/** 인생 기록·졸업 요약 담당이 같은 저장 결과를 조회하는 계약입니다. */
public record FieldLearningSummaryView(FieldLearningView program, List<Milestone> milestones) {
    public record Milestone(String eventCode, String path, String resultCode, Map<String, Object> evidence) { }
}

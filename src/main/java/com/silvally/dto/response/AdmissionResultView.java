package com.silvally.dto.response;

import java.util.List;
import java.util.Map;

/** 판정 당시의 불변 결과. 현재 회차가 다음 단계로 진행해도 재평가하지 않습니다. */
public record AdmissionResultView(String eventCode, String resultCode, String admissionTrack, String appliedTrack,
                                  Decision decision, Map<String, Double> componentScores,
                                  SchoolInterviewEvaluationView interview, List<String> reasonCodes,
                                  String nextPhase, Integer beforeSchoolDay) {
    public record Decision(boolean passed, boolean shieldActivated, int rank, int capacity,
                           double score, double generalScore, int generalRank) { }
}

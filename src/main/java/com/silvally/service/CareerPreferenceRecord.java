package com.silvally.service;

import com.silvally.dto.response.CareerPreferenceView;

import java.util.List;
import java.util.Map;

/**
 * 플레이어에 저장하는 희망 직무 기록. 최초 추천 당시의 능력치와 추천 결과, 확정·변경 이력을 보존한다.
 */
public record CareerPreferenceRecord(
        Map<String, Integer> stats,
        List<CareerPreferenceView.Recommendation> recommendations,
        List<Change> history
) {

    /** type은 SELECT(최초 확정) 또는 CHANGE(변경). */
    public record Change(String type, String fromRole, String toRole, int year, int turn) {
    }
}

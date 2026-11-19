package com.silvally.service;

import java.util.List;

/**
 * 프로젝트 카탈로그 한 건의 규칙.
 *
 * @param role           포트폴리오 점수를 환산할 때 일치로 보는 직무 코드
 * @param durationTurns  시작 턴을 포함해 작업할 수 있는 턴 수
 * @param workRequired   완성도를 계산하기 위해 필요한 작업 스냅샷 수
 * @param focusStats     작업 스냅샷에 반영하는 능력치
 * @param passQuality    완료로 인정하는 최소 완성도
 */
public record ProjectRule(
        String code,
        String title,
        String role,
        int minYear,
        int durationTurns,
        int workRequired,
        List<String> focusStats,
        int passQuality
) {
}

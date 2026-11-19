package com.silvally.service;

import java.util.List;

/**
 * 기업 카탈로그 한 건의 채용 규칙.
 *
 * @param deadlineTurn  지원·전형을 마칠 수 있는 마지막 누적 턴(1학년 1턴 = 1)
 * @param docPass       서류 통과 최소 점수
 * @param codingPass    코딩 테스트 통과 최소 점수
 * @param interviewPass 회사 면접 통과 최소 점수
 * @param offerPass     최종 결과에서 오퍼를 받는 최소 종합 점수
 */
public record CompanyRule(
        String code,
        String title,
        String role,
        int minYear,
        int deadlineTurn,
        int docPass,
        int codingPass,
        int interviewPass,
        int offerPass,
        List<String> codingStats,
        List<String> interviewStats
) {
}

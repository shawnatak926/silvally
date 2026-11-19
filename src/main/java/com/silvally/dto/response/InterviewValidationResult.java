package com.silvally.dto.response;

import java.util.UUID;

/** 채용 서비스는 이 검증 결과를 받아 회사 전형의 점수·상태를 자체 처리합니다. */
public record InterviewValidationResult(UUID questionId, String kind, String subjectCode,
                                        String question, String answer, String source) { }

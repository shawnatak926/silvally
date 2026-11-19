package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * 채용 단계 진행 요청. INTERVIEW 단계는 면접 질문 조회로 발급받은 questionId와 답변이 필요하다.
 */
public record JobStepRequest(
        @NotBlank String step,
        UUID questionId,
        String answer
) {
    public JobStepRequest(String step) {
        this(step, null, null);
    }
}

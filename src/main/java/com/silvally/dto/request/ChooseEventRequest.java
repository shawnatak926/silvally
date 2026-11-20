package com.silvally.dto.request;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record ChooseEventRequest(String choiceCode, List<String> choiceCodes, java.util.UUID questionId, String answer,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String motivation,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) NarshaInput narsha) {
    public ChooseEventRequest(String choiceCode, List<String> choiceCodes, java.util.UUID questionId, String answer, String motivation) {
        this(choiceCode, choiceCodes, questionId, answer, motivation, null);
    }
    public ChooseEventRequest(String choiceCode, List<String> choiceCodes, java.util.UUID questionId, String answer) {
        this(choiceCode, choiceCodes, questionId, answer, null, null);
    }
    /** 나르샤 프로젝트 시작처럼 주제·역할 입력이 붙는 단일 선택. */
    public static ChooseEventRequest withNarsha(String choiceCode, NarshaInput narsha) {
        return new ChooseEventRequest(choiceCode, null, null, null, null, narsha);
    }
    public ChooseEventRequest(String choiceCode, List<String> choiceCodes) { this(choiceCode, choiceCodes, null, null); }
    public ChooseEventRequest(String choiceCode) { this(choiceCode, null); }
    @AssertTrue(message = "choiceCode 또는 choiceCodes 중 하나만 지정해야 합니다.")
    public boolean isSelectionValid() {
        if (narsha != null && (choiceCode == null || choiceCode.isBlank() || choiceCodes != null || questionId != null
                || answer != null || motivation != null)) return false;
        if (motivation != null && (motivation.isBlank() || motivation.length() > 1200 || choiceCodes != null
                || "ANSWER".equals(choiceCode))) return false;
        if ("ANSWER".equals(choiceCode)) return choiceCodes == null && questionId != null
                && answer != null && !answer.isBlank() && answer.length() <= 1200;
        if (questionId != null || answer != null) return false;
        if (choiceCode != null) return choiceCodes == null && !choiceCode.isBlank();
        return choiceCodes != null && !choiceCodes.isEmpty()
                && choiceCodes.stream().allMatch(code -> code != null && !code.isBlank());
    }
}

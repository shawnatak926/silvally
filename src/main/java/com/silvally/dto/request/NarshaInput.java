package com.silvally.dto.request;

/**
 * 나르샤 시작 선택에 붙는 주제·역할 입력.
 *
 * @param topicCode   서버가 제시한 후보 코드 또는 {@code CUSTOM}(기타 직접 입력)
 * @param customTopic {@code CUSTOM}일 때만 보내는 직접 입력 주제(공백 정리 후 2~80자)
 * @param role        팀 안의 역할 코드(선택, 생략하면 팀원)
 */
public record NarshaInput(String topicCode, String customTopic, String role) {
}

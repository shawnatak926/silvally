package com.silvally.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_TARGET(HttpStatus.BAD_REQUEST, "행동 대상 형식이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INSUFFICIENT_AP(HttpStatus.CONFLICT, "행동 포인트(AP)가 부족합니다."),
    LOCKED(HttpStatus.CONFLICT, "현재 조건에서는 선택할 수 없습니다."),
    WRONG_PHASE(HttpStatus.CONFLICT, "현재 단계에서는 요청할 수 없습니다."),
    WRONG_STEP(HttpStatus.CONFLICT, "현재 전형 단계에서는 요청할 수 없습니다."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "회차가 이미 변경되었습니다. 최신 상태를 다시 조회하세요."),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "같은 요청 키로 다른 내용의 요청이 들어왔습니다."),
    FINAL_PATH_REQUIRED(HttpStatus.CONFLICT, "오퍼가 있어 최종 진로를 직접 선택해야 합니다."),
    ENDING_NOT_READY(HttpStatus.CONFLICT, "아직 엔딩을 조회할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return name();
    }

    public String getMessage() {
        return message;
    }
}

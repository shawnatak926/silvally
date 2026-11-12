package com.silvally.common;

import com.silvally.common.exception.ErrorCode;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, Object> details) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), Map.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, Map.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
        return new ErrorResponse(errorCode.getCode(), message, details);
    }
}

package com.silvally.common;

import com.silvally.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> errors,
        LocalDateTime timestamp
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(),
                errorCode.getMessage(), List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(),
                message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(),
                errorCode.getMessage(), errors, LocalDateTime.now());
    }
}

package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * 졸업 요청의 진로 코드. AUTO는 서버가 상태를 보고 진로를 정한다.
 */
public enum GraduationPath {
    AUTO,
    EMPLOYMENT,
    UNEMPLOYED,
    UNIVERSITY,
    INDIE_DEV;

    public static GraduationPath parse(String raw) {
        if (raw != null) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            for (GraduationPath path : values()) {
                if (path.name().equals(normalized)) {
                    return path;
                }
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "알 수 없는 진로 코드입니다: " + raw,
                Map.of("allowed", Arrays.stream(values()).map(Enum::name).toList()));
    }
}

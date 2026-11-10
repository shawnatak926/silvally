package com.silvally.dto.response;

import java.util.Map;

public record EndingView(
        String endingCode,
        String title,
        String body,
        String selectedPath,
        String acceptedCompany,
        String acceptedRole,
        Map<String, Object> summary
) {
}

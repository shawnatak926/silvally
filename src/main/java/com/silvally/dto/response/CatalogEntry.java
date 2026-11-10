package com.silvally.dto.response;

import java.util.Map;

public record CatalogEntry(
        String code,
        String title,
        String requirements,
        Map<String, Object> details
) {
}

package com.silvally.dto.response;

public record AvailableAction(
        String code,
        String target,
        int cost,
        boolean enabled,
        String reason
) {
}

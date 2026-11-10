package com.silvally.dto.response;

import java.util.Map;

public record MutationResult(
        PlayerView player,
        String actionCode,
        int spentAp,
        Map<String, Integer> statDelta,
        String resultCode,
        Map<String, Object> details
) {
}

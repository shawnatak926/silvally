package com.silvally.dto.response;

import java.util.List;

public record EventView(
        String code,
        String title,
        String situation,
        boolean required,
        List<Choice> choices,
        boolean blocksAuto,
        java.util.Map<String, Object> source
) {
    public EventView(String code, String title, String situation, boolean required, List<Choice> choices) {
        this(code, title, situation, required, choices, required || "J01".equals(code), null);
    }

    public record Choice(String code, String label, int apCost, boolean enabled, String reason, String description) {
        public Choice(String code, String label, int apCost, boolean enabled, String reason) {
            this(code, label, apCost, enabled, reason, null);
        }
    }
}

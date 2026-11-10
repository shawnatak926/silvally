package com.silvally.dto.response;

import java.util.List;

public record EventView(
        String code,
        String title,
        String situation,
        boolean required,
        List<Choice> choices
) {

    public record Choice(String code, String label, int apCost, boolean enabled, String reason) {
    }
}

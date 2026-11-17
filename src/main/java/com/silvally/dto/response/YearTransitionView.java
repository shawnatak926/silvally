package com.silvally.dto.response;

import java.util.List;

public record YearTransitionView(
        int year,
        String pendingChoice,
        List<Option> options
) {
    public record Option(String code, String title, List<String> focus) {
    }
}

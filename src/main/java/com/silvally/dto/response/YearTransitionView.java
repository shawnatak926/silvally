package com.silvally.dto.response;

import java.util.List;

public record YearTransitionView(
        int year,
        String pendingChoice,
        List<Option> options,
        String notice
) {
    public YearTransitionView(int year, String pendingChoice, List<Option> options) {
        this(year, pendingChoice, options, null);
    }

    public record Option(String code, String title, List<String> focus) {
    }
}

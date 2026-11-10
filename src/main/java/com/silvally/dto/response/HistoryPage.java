package com.silvally.dto.response;

import java.util.List;

public record HistoryPage(
        List<Item> items,
        Long nextAfterId
) {
    public record Item(
            Long logId,
            int year,
            int turn,
            Integer beforeSchoolDay,
            String kind,
            String subjectCode,
            String choiceCode,
            String resultCode
    ) {
    }
}

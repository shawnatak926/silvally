package com.silvally.repository;

public record HistoryRow(
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

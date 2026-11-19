package com.silvally.repository;

import com.silvally.entity.NarrativeSource;
import com.silvally.entity.NarrativeStatus;

public record HistoryRow(
        Long logId,
        int year,
        int turn,
        Integer beforeSchoolDay,
        String kind,
        String subjectCode,
        String choiceCode,
        String resultCode,
        NarrativeStatus narrativeStatus,
        String narrative,
        NarrativeSource narrativeSource,
        String narrativeModel
) {
}

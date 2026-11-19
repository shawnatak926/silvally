package com.silvally.service;

import java.util.List;

public record NarrativeScene(
        String sceneId,
        String title,
        String situation,
        String choiceCode,
        String choiceLabel,
        String resultCode,
        String evidenceJson,
        int year,
        int turn,
        Integer beforeSchoolDay,
        NarrativeContext context,
        List<String> previousNarratives
) {
}

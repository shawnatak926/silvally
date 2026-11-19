package com.silvally.service;

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
        Integer beforeSchoolDay
) {
}

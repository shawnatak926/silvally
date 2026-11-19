package com.silvally.service;

import com.silvally.entity.NarrativeSource;

public class TemplateNarrativeGenerator implements NarrativeGenerator {

    @Override
    public GeneratedNarrative generate(NarrativeScene scene) {
        StringBuilder text = new StringBuilder();
        text.append(when(scene)).append(", '").append(scene.title()).append("' 장면. ");
        if (scene.situation() != null && !scene.situation().isBlank()) {
            text.append(scene.situation()).append(' ');
        }
        text.append("나는 '").append(scene.choiceLabel()).append("'을(를) 선택했고, 결과는 ")
                .append(scene.resultCode()).append("였다.");
        return new GeneratedNarrative(text.toString(), NarrativeSource.TEMPLATE, null);
    }

    private static String when(NarrativeScene scene) {
        if (scene.beforeSchoolDay() != null) {
            return "입학 전 " + scene.beforeSchoolDay() + "일차";
        }
        if (scene.year() == 0) {
            return "입학 전형 중";
        }
        return scene.year() + "학년 " + scene.turn() + "턴";
    }
}
